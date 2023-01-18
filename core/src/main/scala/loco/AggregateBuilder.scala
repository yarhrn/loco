package loco

import loco.domain.{Aggregate, AggregateId, Event, MetaEvent}

/**
 * An AggregateBuilder[A,E] describes a way how to build an aggregate of type A from the events of type E.
 *
 * @tparam A
 *   type of an aggregate build from events of type E
 * @tparam E
 *   type of the events
 */
trait AggregateBuilder[A <: Aggregate[E], E <: Event] {

  /**
   * Builds 'empty' aggregate from the given `id`
   */
  def empty(id: AggregateId[E]): A

  /**
   * Returns new aggregate as a the result of applying given `metaEvent` on the given `aggregate`
   */
  def apply(aggregate: A, metaEvent: MetaEvent[E]): A
}
