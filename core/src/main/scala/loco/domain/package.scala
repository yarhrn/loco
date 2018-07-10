package loco

import java.time.Instant

import cats.data.NonEmptyList

package object domain {

  trait Event

  case class AggregateId[E <: Event](id: String)

  case class AggregateVersion[E <: Event](version: Int)


  case class MetaEvent[E <: Event](aggregateId: AggregateId[E],
                          domainEvent: E,
                          createdAt: Instant,
                          version: AggregateVersion[E])

  //                   eventName: String,
  //                   eventMetadataLString)

  object MetaEvent {
    def fromRawEvents[E <: Event](aggregateId: AggregateId[E],
                         instant: Instant,
                         lastKnownVersion: AggregateVersion[E],
                         events: NonEmptyList[E]): NonEmptyList[MetaEvent[E]] = {
      import cats.implicits._

      val versions = (1 to events.size).map(counter => AggregateVersion[E](lastKnownVersion.version + counter))

      val listEvents = events.toList.zip(versions).map {
        case (event, version) =>
          MetaEvent(aggregateId, event, instant, version)
      }

      NonEmptyList.fromListUnsafe(listEvents)
    }
  }


}
