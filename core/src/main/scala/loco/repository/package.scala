package loco

import cats.data.NonEmptyList
import loco.domain.{AggregateId, AggregateVersion, MetaEvent}
import monix.tail.Iterant

package object repository {

  trait EventsRepository[F[_], E] {

    def fetchEvents(id: AggregateId[E], version: Option[AggregateVersion[E]] = None): Iterant[F, MetaEvent[E]]

    def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit]

  }

}
