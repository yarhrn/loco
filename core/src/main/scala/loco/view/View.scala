package loco.view

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Monad, MonadError}
import loco.ErrorReporter
import loco.ErrorReporter._
import loco.domain._
import loco.repository.EventsRepository

import scala.language.higherKinds

trait View[F[_], E <: Event] {
  def handle(events: NonEmptyList[MetaEvent[E]]): F[Unit]
}

trait EventViewPF[F[_], E <: Event] {
  val handle: PartialFunction[E, F[Unit]]
}

trait MetaEventView[F[_], E <: Event] {
  def handle(metaEvent: MetaEvent[E]): F[Unit]
}

trait MetaEventViewWithAggregate[F[_], E <: Event, A <: Aggregate[E]] {
  def handle(metaEvent: MetaEvent[E], metaAggregate: MetaAggregate[E, A]): F[Unit]
}

object View {

  def wrap[F[_], E <: Event](views: List[View[F, E]])(implicit ME: MonadError[F, Throwable], ER: ErrorReporter[F]) =
    new View[F, E] {

      override def handle(events: NonEmptyList[MetaEvent[E]]) = {
        views.map(_.handle(events).reportError).sequence.void
      }

    }

  def wrap[F[_], E <: Event](eventView: EventViewPF[F, E])(implicit M: MonadError[F, Throwable], ER: ErrorReporter[F]) =
    new View[F, E] {
      override def handle(events: NonEmptyList[MetaEvent[E]]) = {
        events
          .toList
          .map { event => eventView.handle.applyOrElse(event.event, (_: E) => Monad[F].unit).reportError }
          .sequence
          .void
      }
    }

  def wrap[F[_], E <: Event](
      metaEventView: MetaEventView[F, E])(implicit M: MonadError[F, Throwable], ER: ErrorReporter[F]) =
    new View[F, E] {
      override def handle(events: NonEmptyList[MetaEvent[E]]) = {
        events.toList.map { event => metaEventView.handle(event).reportError }.sequence.void
      }
    }

  def wrap[F[_], E <: Event, A <: Aggregate[E]](
      views: List[MetaEventViewWithAggregate[F, E, A]])(implicit ME: MonadError[F, Throwable], ER: ErrorReporter[F]) =
    new MetaEventViewWithAggregate[F, E, A] {
      override def handle(metaEvent: MetaEvent[E], metaAggregate: MetaAggregate[E, A]) = {
        views.traverse(view => view.handle(metaEvent, metaAggregate).reportError).void
      }
    }

  def wrap[F[_], E <: Event, A <: Aggregate[E]](
      view: MetaEventViewWithAggregate[F, E, A],
      metaAggregateBuilder: MetaAggregateBuilder[E, A],
      eventsRepository: EventsRepository[F, E])(implicit S: Sync[F], ER: ErrorReporter[F]) =
    new View[F, E] {

      override def handle(events: NonEmptyList[MetaEvent[E]]) = {
        val id = events.head.aggregateId

        for {
          initialMetaAggregate <- eventsRepository
            .fetchEvents(id, events.head.version.decrement)
            .compile
            .fold(metaAggregateBuilder.empty(id))(metaAggregateBuilder.apply)
          _ <- events.foldLeftM(initialMetaAggregate) { (aggregate, metaEvent) =>
            view.handle(metaEvent, aggregate).reportError *> S.pure(metaAggregateBuilder.apply(aggregate, metaEvent))
          }
        } yield {
          ()
        }
      }
    }

  def empty[F[_]: Applicative, E <: Event]: View[F, E] = (_: NonEmptyList[MetaEvent[E]]) => ().pure[F]
}
