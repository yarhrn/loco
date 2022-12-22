package loco.repository.persistent.mongo

import java.nio.charset.StandardCharsets
import java.util.Date

import cats.MonadError
import cats.data.NonEmptyList
import cats.effect.{ConcurrentEffect, ContextShift}
import cats.implicits._
import com.mongodb.MongoBulkWriteException
import com.mongodb.client.model.{Filters, Sorts}
import com.mongodb.reactivestreams.client.MongoCollection
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import loco.repository.EventsRepository
import loco.repository.EventsRepository.ConcurrentModificationException
import loco.repository.persistent.Codec
import loco.repository.persistent.mongo.MongoDBFReactiveFS2._
import org.bson.Document
import org.bson.types.ObjectId

import scala.collection.JavaConverters._

class MongoDBEventsRepository[F[_] : ConcurrentEffect, E <: Event : Codec](col: MongoCollection[Document],
                                                                           cs: ContextShift[F]) extends EventsRepository[F, E] {

  val createdAtField = "createdAt"
  val eventField = "event"
  val versionField = "version"
  val aggregateIdField = "aggregateId"

  override def fetchEvents(id: AggregateId[E], version: AggregateVersion[E]) = {
    val criteria = Filters.and(Filters.eq(aggregateIdField, id.id), Filters.lte(versionField, version.version))
    fs2.interop.reactivestreams.fromPublisher(col.find(criteria)
      .sort(Sorts.ascending(versionField))).map { document: Document =>
      val createdAt = document.getDate(createdAtField).toInstant
      val version = document.getInteger(versionField)
      val event = Codec[E].decode(document.get(eventField).asInstanceOf[Document].toJson.getBytes(StandardCharsets.UTF_8))
      val aggregateId = document.getString(aggregateIdField)
      MetaEvent(
        AggregateId(aggregateId),
        event,
        createdAt,
        AggregateVersion(version)
      )
    }.evalTap(_ => cs.shift)
  }


  override def saveEvents(events: NonEmptyList[MetaEvent[E]]) = {


    val eventsList = events.sortBy(_.version.version).toList
    val documents = eventsList.map { event =>
      val id = ObjectId.get()
      (id, new Document()
        .append("_id", id)
        .append(createdAtField, Date.from(event.createdAt))
        .append(versionField, event.version.version)
        .append(aggregateIdField, event.aggregateId.id)
        .append(eventField, Document.parse(new String(Codec[E].encode(event.event), StandardCharsets.UTF_8))))
    }

    val insertion: F[Unit] = col.effect[F].insertMany(documents.map(_._2)).attempt.flatMap {
      case Right(_) => ().pure[F]
      case Left(ex: MongoBulkWriteException) =>
        val insertedDocs = Filters.in("_id", documents.map(_._1).take(ex.getWriteResult.getInsertedCount).asJava)
        col.effect[F].deleteMany(insertedDocs) *> MonadError[F, Throwable].raiseError(new ConcurrentModificationException(
          eventsList.head.aggregateId,
          eventsList.map(_.version)
        ))
      case Left(ex) => MonadError[F, Throwable].raiseError(ex)
    }

    insertion.attempt.flatTap(_ => cs.shift).rethrow
  }
}

