package loco.repository

import cats.data.NonEmptyList
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import scala.language.higherKinds

trait EventsRepository[F[_], E <: Event] {

  def fetchEvents(id: AggregateId[E], version: AggregateVersion[E] = AggregateVersion.max): fs2.Stream[F, MetaEvent[E]]

  def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit]

}

object EventsRepository{
  class ConcurrentModificationException() extends RuntimeException
}