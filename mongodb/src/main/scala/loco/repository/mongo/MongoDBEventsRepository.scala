package loco.repository.mongo

import java.util.Date

import cats.MonadError
import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits._
import com.mongodb.MongoBulkWriteException
import com.mongodb.async.client.MongoCollection
import com.mongodb.client.model.Filters
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import loco.repository.EventsRepository
import loco.repository.EventsRepository.ConcurrentModificationException
import loco.repository.mongo.MongoDBFS2._
import loco.repository.persistent.sql.Codec
import org.bson.{BsonObjectId, Document}
import scala.collection.JavaConverters._

class MongoDBEventsRepository[F[_] : Async, E <: Event : Codec](col: MongoCollection[Document]) extends EventsRepository[F, E] {

  val createdAtField = "createdAt"
  val eventField = "event"
  val versionField = "version"
  val aggregateIdField = "aggregateId"

  override def fetchEvents(id: AggregateId[E], version: AggregateVersion[E]) = {
    col.find(new Document().append("aggregateId", id.id)).stream.map { document: Document =>
      val createdAt = document.getDate(createdAtField).toInstant
      val version = document.getInteger(versionField)
      val event = Codec[E].decode(document.get(eventField).asInstanceOf[Document].toJson)
      val aggregateId = document.getString(aggregateIdField)
      MetaEvent(
        AggregateId(aggregateId),
        event,
        createdAt,
        AggregateVersion(version)
      )
    }
  }


  override def saveEvents(events: NonEmptyList[MetaEvent[E]]) = {
    import org.bson.Document
    import org.bson.types.ObjectId


    val documents = events.sortBy(_.version.version).toList.map { event =>
      val id = ObjectId.get()
      (id, new Document()
        .append("_id", id)
        .append(createdAtField, Date.from(event.createdAt))
        .append(versionField, event.version.version)
        .append(aggregateIdField, event.aggregateId.id)
        .append(eventField, Document.parse(Codec[E].encode(event.event))))
    }

    col.effect[F].insertMany(documents.map(_._2)).attempt.flatMap {
      case Right(_) => ().pure[F]
      case Left(ex: MongoBulkWriteException) =>
        val insertedDocs = Filters.in("_id", documents.map(_._1).take(ex.getWriteResult.getInsertedCount).asJava)

        col.effect[F].deleteMany(insertedDocs) *> MonadError[F, Throwable].raiseError(new ConcurrentModificationException())
      case Left(ex) => MonadError[F, Throwable].raiseError(ex)
    }
  }
}

