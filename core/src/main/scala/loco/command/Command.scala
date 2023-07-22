package loco.command

import cats.Applicative
import cats.data.Chain
import loco.domain.{Aggregate, Event}

trait Command[F[_], E <: Event, A <: Aggregate[E], R] {
  def events(a: A): F[CommandResult[E, R]]
}

object Command {
  def pure[F[_] : Applicative, E <: Event, A <: Aggregate[E], R](c: A => CommandResult[E, R]) = {
    new Command[F, E, A, R] {
      override def events(a: A) = Applicative[F].pure(c(a))
    }
  }
}

sealed trait CommandResult[E <: Event, R]

case class FailedCommand[E <: Event, R](th: Throwable, events: Chain[E] = Chain()) extends CommandResult[E, R]

case class SuccessCommand[E <: Event, R](commandResult: R, events: Chain[E] = Chain()) extends CommandResult[E, R] {
  def result[A](a: A): SuccessCommand[E, A] = copy(commandResult = a)

  def failed(th: Throwable): FailedCommand[E, R] = FailedCommand(th, events)
}

object CommandResult {

  def events[E <: Event](events: List[E]): SuccessCommand[E, Unit] = {
    SuccessCommand((), Chain.fromSeq(events))
  }

  def events[E <: Event](events: E*): SuccessCommand[E, Unit] = {
    SuccessCommand((), Chain(events:_*))
  }
}
