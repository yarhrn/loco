package loco

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.{IO, Sync, Timer}
import monix.tail.Iterant

import scala.language.higherKinds

class AggregateId[E](id: String)

case class AggregateVersion[E](version: Int)


case class MetaEvent[E](aggregateId: AggregateId[E],
                        domainEvent: E,
                        createdAt: Instant,
                        version: AggregateVersion[E])

//                   eventName: String,
//                   eventMetadataLString)

object MetaEvent {
  def of[E](aggregateId: AggregateId[E],
            instant: Instant,
            lastAggregateVersion: AggregateVersion[E],
            events: NonEmptyList[E]): NonEmptyList[MetaEvent[E]] = {
    import cats.implicits._

    val versions = (1 to events.size).map(counter => AggregateVersion[E](lastAggregateVersion.version + counter))

    val listEvents = events.toList.zip(versions).map {
      case (event, version) =>
        MetaEvent(aggregateId, event, instant, version)
    }

    NonEmptyList.fromListUnsafe(listEvents)
  }
}

trait EventsRepository[F[_], E] {

  def fetchEvents(id: AggregateId[E], version: Option[AggregateVersion[E]] = None): Iterant[F, MetaEvent[E]]

  def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit]

}

trait ErrorReporter[F[_]] {
  def error(throwable: Throwable): F[Unit]
}

trait View[F[_], E] {
  def handle(event: MetaEvent[E]): F[Unit]
}

trait ViewWithEvents[F[_], E] {
  def handle(event: MetaEvent[E], events: Iterant[F, MetaEvent[E]]): F[Unit]
}

trait ViewWithAggregate[F[_], A, E] {
  def handle(event: MetaEvent[E], aggregate: A): F[Unit]
}

trait AggregatorBuilder[A, E] {
  def empty: A

  def apply(aggregate: A, metaEvent: MetaEvent[E]): A
}


class ES[F[_], E, A](aggregatorBuilder: AggregatorBuilder[A, E],
                     repository: EventsRepository[F, E],
                     views: List[View[F, E]],
                     viewsWithAggregates: List[ViewWithAggregate[F, A, E]],
                     viewsWithEvents: List[ViewWithEvents[F, E]],
                     errorReporter: ErrorReporter[F])
                    (implicit timer: Timer[F], monad: Sync[F]) {

  import cats.implicits._

  def saveEvents(id: AggregateId[E], version: AggregateVersion[E], events: NonEmptyList[E]): F[Unit] = {

    for {
      instant <- Timer[F].clockRealTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)
      metaEvents = MetaEvent.of(id, instant, version, events)
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
    val z: List[F[Unit]] = for {
      view <- views
      event <- events
    } yield {
      view.handle(event)
    }
    z.sequence.unitify
  }

  private def notifyViewsWithEvents(id: AggregateId[E], events: List[MetaEvent[E]]): F[Unit] = {
    val z: List[F[Unit]] = for {
      view <- viewsWithEvents
      event <- events
    } yield {
      val allEvents = repository.fetchEvents(id, Some(event.version))
      view.handle(event, allEvents).recoverWith {
        case ex => errorReporter.error(ex)
      }
    }
    z.sequence.unitify
  }

  private def notifyWithAggregate(id: AggregateId[E], events: List[MetaEvent[E]]): F[Unit] = {

    val z: List[F[Unit]] = for {
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
    z.sequence.unitify
  }

  def buildAggregate(id: AggregateId[E], version: AggregateVersion[E]) = {
    repository.fetchEvents(id, Some(version)).foldLeftL(aggregatorBuilder.empty)((aggregate, event) => aggregatorBuilder(aggregate, event))
  }

  implicit class Unitify[F[_] : Monad, A](fa: F[A]) {
    def unitify = fa *> Monad[F].unit
  }


}

object Main {

  class Account

  class AccountEvent

  val e: EventsRepository[IO, AccountEvent] = ???

  val value: IO[Account] = e.fetchEvents(???).foldLeftL((??? : Account))((_, _) => (??? : Account))
}