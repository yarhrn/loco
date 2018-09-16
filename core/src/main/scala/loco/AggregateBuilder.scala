package loco

import loco.domain.{Aggregate, AggregateId, Event, MetaEvent}

/**
  * An AggregateBuilder[A,E] describes a way how to build aggregate of type A from events of type E.
  *
  * @tparam A type of aggregate build from events of type E
  * @tparam E type of events
  */
trait AggregateBuilder[A <: Aggregate[E], E <: Event] {
  /**
    * Builds 'empty' aggregate from given `id`
    */
  def empty(id: AggregateId[E]): A


  /**
    * Returns new aggregate as a the result of applying given `metaEvent` on give `aggregate`
    */
  def apply(aggregate: A, metaEvent: MetaEvent[E]): A
}