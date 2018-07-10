package loco

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.{Functor, Monad}
import cats.data.NonEmptyList
import cats.effect.{Sync, Timer}
import loco.domain._
import loco.repository.EventsRepository
import loco.view._
import monix.tail.Iterant

import scala.language.higherKinds


trait ErrorReporter[F[_]] {
  def error(throwable: Throwable): F[Unit]
}


trait AggregateBuilder[A <: Aggregate[E], E <: Event] {
  def empty: (AggregateId[E] => A)

  def apply(aggregate: A, metaEvent: MetaEvent[E]): A
}

trait EventSourcing[F[_], E <: Event, A <: Aggregate[E]] {
  def saveEvents(events: NonEmptyList[E])(implicit f: Functor[F]): F[AggregateId[E]] = {
    import cats.implicits._
    val id = AggregateId[E](UUID.randomUUID().toString)
    val version = AggregateVersion[E](0)
    saveEvents(id, version, events).map(_ => id)
  }

  def saveEvents(id: AggregateId[E], lastKnownVersion: AggregateVersion[E], events: NonEmptyList[E]): F[Unit]

  def fetchAggregate(id: AggregateId[E]): F[Option[A]]
}

class ES[F[_], E <: Event, A <: Aggregate[E]](aggregateBuilder: AggregateBuilder[A, E],
                                              repository: EventsRepository[F, E],
                                              views: List[View[F, E]],
                                              viewsWithAggregates: List[ViewWithAggregate[F, A, E]],
                                              viewsWithEvents: List[ViewWithEvents[F, E]],
                                              errorReporter: ErrorReporter[F])
                                             (implicit timer: Timer[F], monad: Sync[F]) extends EventSourcing[F, E, A] {

  import cats.implicits._

  override def saveEvents(id: AggregateId[E], lastKnownVersion: AggregateVersion[E], events: NonEmptyList[E]): F[Unit] = {

    for {
      instant <- Timer[F].clockRealTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)
      metaEvents = MetaEvent.fromRawEvents(id, instant, lastKnownVersion, events)
      _ <- repository.saveEvents(metaEvents)

      metaEventsList = metaEvents.toList

      _ <- List(
        notifyViews(metaEventsList),
        notifyViewsWithEvents(id, metaEventsList),
        notifyWithAggregate(id, metaEventsList))
        .sequence
        .unitify
        .recoverWith {
          case ex => errorReporter.error(ex)
        }

    } yield {

      ()
    }
  }

  private def notifyViews(events: List[MetaEvent[E]]): F[Unit] = {
    val actions: List[F[Unit]] = for {
      view <- views
      event <- events
    } yield {
      view.handle(event)
    }
    actions.sequence.unitify.adaptError {
      case ex => new RuntimeException(ex)
    }
  }

  private def notifyViewsWithEvents(id: AggregateId[E], events: List[MetaEvent[E]]): F[Unit] = {
    val actions: List[F[Unit]] = for {
      event <- events

      view <- viewsWithEvents
    } yield {
      val allEvents: Iterant[F, MetaEvent[E]] = repository.fetchEvents(id, Some(event.version)) // TODO we can optimize for small amount of events
      view.handle(event, allEvents).recoverWith {
        case ex => errorReporter.error(ex)
      }
    }
    actions.sequence.unitify.adaptError {
      case ex => new RuntimeException(ex)
    }
  }

  private def notifyWithAggregate(id: AggregateId[E], events: List[MetaEvent[E]]): F[Unit] = {

    if (views.nonEmpty) {
      val actions: List[F[Unit]] = for {
        event <- events
      } yield {
        {
          for {
            aggregate <- buildAggregate(id, event.version)
            _ <- viewsWithAggregates.map(view => view.handle(event, aggregate)).sequence
          } yield ()
        }.recoverWith {
          case ex => errorReporter.error(ex)
        }

      }
      actions.sequence.unitify.adaptError {
        case ex => new RuntimeException(ex)
      }
    } else {
      Monad[F].unit
    }
  }

  def buildAggregate(id: AggregateId[E], version: AggregateVersion[E]): F[A] = {
    repository.fetchEvents(id, Some(version)).foldLeftL(aggregateBuilder.empty(id))((aggregate, event) => aggregateBuilder(aggregate, event))
  }

  implicit class Unitify[F[_] : Functor, A](fa: F[A]) {
    def unitify: F[Unit] = fa.map(_ => Unit)
  }

  override def fetchAggregate(id: AggregateId[E]): F[Option[A]] = {
    val builder: MetaAggregateBuilder[E, A] = new MetaAggregateBuilder[E, A](aggregateBuilder)
    repository.fetchEvents(id).foldLeftL(builder.empty(id))((agr, event) => builder(agr, event)).map {
      metaAggregate =>
        if (metaAggregate.aggregateVersion.version == 0) {
          None
        } else {
          Some(metaAggregate.aggregate)
        }
    }

  }
}