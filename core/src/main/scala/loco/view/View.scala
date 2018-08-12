package loco.view

import cats.{Monad, MonadError}
import cats.data.NonEmptyList
import loco.ErrorReporter
import loco.domain._
import cats.implicits._
import javax.swing.text.CompositeView

import scala.language.higherKinds
import loco.util._

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

trait MetaEventViewPF[F[_], E <: Event] {
  val handle: PartialFunction[MetaEvent[E], F[Unit]]
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

  def lift[F[_], E <: Event](pf: MetaEventViewPF[F, E], errorReporter: ErrorReporter[F])
                            (implicit M: MonadError[F, Throwable]) = new View[F, E] {
    override def handle(events: NonEmptyList[MetaEvent[E]]) = {
      events.toList.map { event =>
        pf.handle.applyOrElse(event, (_: MetaEvent[E]) => Monad[F].unit).recoverWith { case ex => errorReporter.error(ex) }
      }.sequence.unitify
    }
  }
}