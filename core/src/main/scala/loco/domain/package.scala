package loco

import java.time.Instant

import cats.data.NonEmptyList

package object domain {


  case class AggregateId[E](id: String)

  case class AggregateVersion[E](version: Int)


  case class MetaEvent[E](aggregateId: AggregateId[E],
                          domainEvent: E,
                          createdAt: Instant,
                          version: AggregateVersion[E])

  //                   eventName: String,
  //                   eventMetadataLString)

  object MetaEvent {
    def fromRawEvents[E](aggregateId: AggregateId[E],
                         instant: Instant,
                         lastAggregateVersion: AggregateVersion[E],
                         events: NonEmptyList[E]): NonEmptyList[MetaEvent[E]] = {
      import cats.implicits._

      val versions = (1 to events.size).map(counter => AggregateVersion[E](lastAggregateVersion.version + counter))

      val listEvents = events.toList.zip(versions).map {
        case (event, version) =>
          MetaEvent(aggregateId, event, instant, version)
      }

      NonEmptyList.fromListUnsafe(listEvents)
    }
  }


}
