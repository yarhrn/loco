package loco.repository

import cats.data.NonEmptyList
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}

import scala.language.higherKinds

trait EventsRepository[F[_], E <: Event] {

  def fetchEvents(id: AggregateId[E], version: AggregateVersion[E] = AggregateVersion.max): fs2.Stream[F, MetaEvent[E]]

  def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit]

}

object EventsRepository {

  class ConcurrentModificationException[E <: Event](aggregateId: AggregateId[E], versions: List[AggregateVersion[E]])
      extends RuntimeException(
        s"concurrent modification exception has occurred while saving events for ${aggregateId.id}, versions are ${versions
            .map(_.version)}"
      )

}
