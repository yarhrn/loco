package loco

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import cats.Functor
import cats.data.NonEmptyList
import cats.effect.{Sync, Timer}
import loco.domain._
import loco.repository.EventsRepository
import loco.view._
import scala.language.higherKinds

trait EventSourcing[F[_], E <: Event, A <: Aggregate[E]] {
  def saveEvents(events: NonEmptyList[E])(implicit f: Functor[F]): F[AggregateId[E]] = {
    import cats.implicits._
    val id = AggregateId[E](UUID.randomUUID().toString)
    val version = AggregateVersion[E](0)
    saveEvents(id, version, events).map(_ => id)
  }

  def saveEvents(id: AggregateId[E], lastKnownVersion: AggregateVersion[E], events: NonEmptyList[E]): F[Unit]

  def fetchMetaAggregate(id: AggregateId[E]): F[Option[MetaAggregate[E, A]]]
}

class ES[F[_], E <: Event, A <: Aggregate[E]](aggregateBuilder: AggregateBuilder[A, E],
                                              repository: EventsRepository[F, E],
                                              views: List[View[F, E]],
                                              viewsWithAggregates: List[ViewWithAggregate[F, A, E]],
                                              viewsWithEvents: List[ViewWithEvents[F, E]],
                                              errorReporter: ErrorReporter[F])
                                             (implicit timer: Timer[F], monad: Sync[F]) extends EventSourcing[F, E, A] {
  private val viewRunner = ViewRunner(views, viewsWithAggregates, viewsWithEvents, repository, aggregateBuilder, errorReporter)

  import cats.implicits._

  override def saveEvents(id: AggregateId[E], lastKnownVersion: AggregateVersion[E], events: NonEmptyList[E]): F[Unit] = {

    for {
      instant <- Timer[F].clockRealTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)
      metaEvents = MetaEvent.fromRawEvents(id, instant, lastKnownVersion, events)
      _ <- repository.saveEvents(metaEvents)
      _ <- viewRunner.notify(metaEvents)
    } yield ()
  }


  def buildAggregate(id: AggregateId[E], version: AggregateVersion[E]): F[A] = {
    repository.fetchEvents(id, Some(version)).foldLeftL(aggregateBuilder.empty(id))((aggregate, event) => aggregateBuilder(aggregate, event))
  }

  override def fetchMetaAggregate(id: AggregateId[E]): F[Option[MetaAggregate[E, A]]] = {
    val builder: MetaAggregateBuilder[E, A] = new MetaAggregateBuilder[E, A](aggregateBuilder)
    repository.fetchEvents(id).foldLeftL(builder.empty(id))((agr, event) => builder(agr, event)).map {
      metaAggregate =>
        if (metaAggregate.aggregateVersion.version == 0) {
          None
        } else {
          Some(metaAggregate)
        }
    }

  }
}