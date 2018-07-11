package loco

import loco.domain.{Aggregate, AggregateId, Event, MetaEvent}

trait AggregateBuilder[A <: Aggregate[E], E <: Event] {
  def empty(id: AggregateId[E]): A

  def apply(aggregate: A, metaEvent: MetaEvent[E]): A
}
