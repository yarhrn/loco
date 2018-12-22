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

case class SuccessCommand[E <: Event, R](result: R, events: Chain[E] = Chain()) extends CommandResult[E, R]


object CommandResult {
  def success[E <: Event](e: E, tail: E*): CommandResult[E, Unit] = {
    SuccessCommand[E, Unit]((), Chain(e) ++ Chain(tail: _*))
  }

  def success[E <: Event, R](r: R, e: E*): CommandResult[E, R] = {
    SuccessCommand[E, R](r, Chain(e: _*))
  }

  def nothing[E <: Event]: CommandResult[E, Unit] = SuccessCommand[E, Unit]((), Chain())

}