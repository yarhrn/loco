package loco.command

import loco.domain.{Aggregate, Event}

trait Command[F[_], E <: Event, A <: Aggregate[E], R] {
  def events(a: Aggregate[E]): F[Either[(Throwable, List[E]), (List[E], R)]]
}