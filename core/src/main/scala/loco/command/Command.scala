package loco.command

import cats.data.NonEmptyList
import loco.domain.{Aggregate, Event}

trait Command[F[_], E <: Event, A <: Aggregate[E], R] {
  def events(a: A): F[CommandResult[E, A, R]]
}

sealed trait CommandResult[E <: Event, A <: Aggregate[E], R]

case class FailedCommand[E <: Event, A <: Aggregate[E], R](th: Throwable, events: List[E] = List()) extends CommandResult[E, A, R]

case class SuccessCommand[E <: Event, A <: Aggregate[E], R](result: R, events: NonEmptyList[E]) extends CommandResult[E, A, R]

case class SuccessUnitCommand[E <: Event, A <: Aggregate[E], R](events: NonEmptyList[E]) extends CommandResult[E, A, R]

object CommandResult {
  def success[E <: Event, A <: Aggregate[E], R](e: E, tail: E*): CommandResult[E, A, R] = {
    SuccessUnitCommand[E, A, R](NonEmptyList.of(e, tail: _*))
  }

  def success[E <: Event, A <: Aggregate[E], R](r: R, e: E, tail: E*): CommandResult[E, A, R] = {
    SuccessCommand(r, NonEmptyList.of[E](e, tail: _*))
  }

}