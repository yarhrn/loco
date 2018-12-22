package loco.command

import cats.Applicative
import cats.data.NonEmptyList
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

case class FailedCommand[E <: Event, R](th: Throwable, events: List[E] = List()) extends CommandResult[E, R]

case class SuccessCommand[E <: Event, R](result: R, events: NonEmptyList[E]) extends CommandResult[E, R]

case class SuccessUnitCommand[E <: Event, R](events: NonEmptyList[E]) extends CommandResult[E, R]


object CommandResult {
  def success[E <: Event, R](e: E, tail: E*): CommandResult[E, R] = {
    SuccessUnitCommand[E, R](NonEmptyList.of(e, tail: _*))
  }

  def success[E <: Event, R](r: R, e: E, tail: E*): CommandResult[E, R] = {
    SuccessCommand(r, NonEmptyList.of[E](e, tail: _*))
  }

}