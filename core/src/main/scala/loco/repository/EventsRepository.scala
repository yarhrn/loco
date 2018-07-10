package loco.repository

import cats.data.NonEmptyList
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import monix.tail.Iterant
import scala.language.higherKinds

trait EventsRepository[F[_], E <: Event] {

  def fetchEvents(id: AggregateId[E], version: Option[AggregateVersion[E]] = None): Iterant[F, MetaEvent[E]]

  def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit]

}