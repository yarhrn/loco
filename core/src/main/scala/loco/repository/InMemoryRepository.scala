package loco.repository

import cats.data.NonEmptyList
import cats.effect.Sync
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import monix.tail.Iterant
import scala.language.higherKinds

case class InMemoryRepository[F[_] : Sync, E <: Event](var storage: Map[AggregateId[E], List[MetaEvent[E]]] = Map.empty[AggregateId[E], List[MetaEvent[E]]]) extends EventsRepository[F, E] {

  override def fetchEvents(id: AggregateId[E], version: Option[AggregateVersion[E]]): Iterant[F, MetaEvent[E]] = {
    Iterant[F]
      .liftF(Sync[F].delay(storage(id).take(version.map(_.version).getOrElse(Integer.MAX_VALUE))))
      .flatMap(Iterant.fromList(_))

  }

  override def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit] = Sync[F].delay {
    val id = events.head.aggregateId
    storage = storage + (id -> storage.getOrElse(id, List()).++(events.toList))
  }
}
