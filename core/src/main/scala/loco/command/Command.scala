package loco.command

import cats.Id
import loco.domain.{Aggregate, Event}

trait Command[F[_], E <: Event, A <: Aggregate[E], R] {
  def events(a: Aggregate[E]): F[Either[(Throwable, List[E]), (List[E], R)]]
}

trait PureCommand[E <: Event, A <: Aggregate[E], R] extends Command[Id,E,A,R]{
  def events(a: Aggregate[E]): Either[(Throwable, List[E]), (List[E], R)]
}

