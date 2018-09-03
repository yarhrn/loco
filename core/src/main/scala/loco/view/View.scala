package loco.view

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import cats.{Monad, MonadError}
import loco.ErrorReporter
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

class CompositeView[F[_], E <: Event](views: List[View[F, E]], errorReporter: ErrorReporter[F])
                                     (implicit ME: MonadError[F, Throwable]) extends View[F, E] {

  override def handle(events: NonEmptyList[MetaEvent[E]]) = {
    views.map { view =>
      view
        .handle(events)
        .recoverWith { case ex => errorReporter.error(ex) }
    }.sequence.void
  }

}

object View {
  private[loco] def wrap[F[_], E <: Event](eventView: EventViewPF[F, E], errorReporter: ErrorReporter[F])
                                          (implicit M: MonadError[F, Throwable]) = new View[F, E] {
    override def handle(events: NonEmptyList[MetaEvent[E]]) = {
      events.toList.map { event =>
        eventView.handle.applyOrElse(event.event, (_: E) => Monad[F].unit).recoverWith { case ex => errorReporter.error(ex) }
      }.sequence.void
    }
  }

  private[loco] def wrap[F[_], E <: Event](metaEventView: MetaEventView[F, E], errorReporter: ErrorReporter[F])
                                          (implicit M: MonadError[F, Throwable]) = new View[F, E] {
    override def handle(events: NonEmptyList[MetaEvent[E]]) = {
      events.toList.map { event =>
        metaEventView.handle(event).recoverWith { case ex => errorReporter.error(ex) }
      }.sequence.void
    }
  }


  private[loco] def wrap[F[_], E <: Event, A <: Aggregate[E]](views: List[MetaEventViewWithAggregate[F, E, A]],
                                                              errorReporter: ErrorReporter[F])
                                                             (implicit ME: MonadError[F, Throwable]) = new MetaEventViewWithAggregate[F, E, A] {
    override def handle(metaEvent: MetaEvent[E], metaAggregate: MetaAggregate[E, A]) = {
      views.traverse(view => view.handle(metaEvent, metaAggregate).recoverWith { case ex => errorReporter.error(ex) }).void
    }
  }


  private[loco] def wrap[F[_], E <: Event, A <: Aggregate[E]](view: MetaEventViewWithAggregate[F, E, A],
                                                              metaAggregateBuilder: MetaAggregateBuilder[E, A],
                                                              eventsRepository: EventsRepository[F, E],
                                                              errorReporter: ErrorReporter[F])
                                                             (implicit S: Sync[F]) = new View[F, E] {

    override def handle(events: NonEmptyList[MetaEvent[E]]) = {
      val id = events.head.aggregateId

      val empty = eventsRepository.fetchEvents(id, events.head.version.decrement).compile.fold(metaAggregateBuilder.empty(id))(metaAggregateBuilder.apply)

      events.foldLeft(empty) {
        (metaAggregateF, metaEvent) =>
          for {
            aggregate <- metaAggregateF
            _ <- view.handle(metaEvent, aggregate).recoverWith { case ex => errorReporter.error(ex) }
          } yield metaAggregateBuilder.apply(aggregate, metaEvent)
      }.void

    }
  }
}