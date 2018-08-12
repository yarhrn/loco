package loco.view

import cats.data.NonEmptyList
import cats.implicits._
import cats.{Monad, MonadError}
import loco.ErrorReporter
import loco.domain._
import loco.util._
import scala.language.higherKinds

trait View[F[_], E <: Event] {
  def handle(events: NonEmptyList[MetaEvent[E]]): F[Unit]
}


class CompositeView[F[_], E <: Event](views: List[View[F, E]], errorReporter: ErrorReporter[F])
                                     (implicit ME: MonadError[F, Throwable]) extends View[F, E] {

  override def handle(events: NonEmptyList[MetaEvent[E]]) = {
    views.map { view =>
      view
        .handle(events)
        .recoverWith { case ex => errorReporter.error(ex) }
    }.sequence.unitify
  }

}

trait EventViewPF[F[_], E <: Event] {
  val handle: PartialFunction[E, F[Unit]]
}

trait MetaEventView[F[_], E <: Event] {
  def handle(metaEvent: MetaEvent[E]): F[Unit]
}

object View {
  def lift[F[_], E <: Event](pf: EventViewPF[F, E], errorReporter: ErrorReporter[F])
                            (implicit M: MonadError[F, Throwable]) = new View[F, E] {
    override def handle(events: NonEmptyList[MetaEvent[E]]) = {
      events.toList.map { event =>
        pf.handle.applyOrElse(event.event, (_: E) => Monad[F].unit).recoverWith { case ex => errorReporter.error(ex) }
      }.sequence.unitify
    }
  }

  def lift[F[_], E <: Event](pf: MetaEventView[F, E], errorReporter: ErrorReporter[F])
                            (implicit M: MonadError[F, Throwable]) = new View[F, E] {
    override def handle(events: NonEmptyList[MetaEvent[E]]) = {
      events.toList.map { event =>
        pf.handle(event).recoverWith { case ex => errorReporter.error(ex) }
      }.sequence.unitify
    }
  }
}