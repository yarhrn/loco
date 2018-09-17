package loco.domain

import java.time.Instant
import java.util.UUID

import cats.data.NonEmptyList
import loco.AggregateBuilder


trait Event

trait Aggregate[E <: Event]

case class AggregateId[E <: Event](id: String)

object AggregateId {
  def random[E <: Event]: AggregateId[E] = AggregateId[E](UUID.randomUUID().toString)
}

case class AggregateVersion[+E <: Event](version: Int) {
  private[loco] def decrement = AggregateVersion(version - 1)
}

object AggregateVersion {
  val none = AggregateVersion[Nothing](0)
  val max = AggregateVersion[Nothing](Int.MaxValue)
}


case class MetaAggregate[E <: Event, A <: Aggregate[E]](aggregate: A, version: AggregateVersion[E])

case class MetaAggregateBuilder[E <: Event, A <: Aggregate[E]](aggregateBuilder: AggregateBuilder[A, E]) {
  def empty(aggregateId: AggregateId[E]) = MetaAggregate[E, A](aggregateBuilder.empty(aggregateId), AggregateVersion(0))

  def apply(aggregate: MetaAggregate[E, A], metaEvent: MetaEvent[E]): MetaAggregate[E, A] = {
    MetaAggregate(aggregateBuilder(aggregate.aggregate, metaEvent), metaEvent.version)
  }
}


//todo add event name and event metadata
case class MetaEvent[E <: Event](aggregateId: AggregateId[E],
                                 event: E,
                                 createdAt: Instant,
                                 version: AggregateVersion[E])

object MetaEvent {
  def fromRawEvents[E <: Event](aggregateId: AggregateId[E],
                                instant: Instant,
                                lastKnownVersion: AggregateVersion[E],
                                events: NonEmptyList[E]): NonEmptyList[MetaEvent[E]] = {

    val versions = (1 to events.size).map(counter => AggregateVersion[E](lastKnownVersion.version + counter))

    val listEvents = events.toList.zip(versions).map {
      case (event, version) =>
        MetaEvent(aggregateId, event, instant, version)
    }

    NonEmptyList.fromListUnsafe(listEvents)
  }
}