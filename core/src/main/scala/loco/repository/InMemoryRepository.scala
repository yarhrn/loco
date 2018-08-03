package loco.repository

import cats.data.NonEmptyList
import cats.effect.Sync
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import scala.language.higherKinds

case class InMemoryRepository[F[_] : Sync, E <: Event](var storage: Map[AggregateId[E], List[MetaEvent[E]]] = Map.empty[AggregateId[E], List[MetaEvent[E]]]) extends EventsRepository[F, E] {

  override def fetchEvents(id: AggregateId[E], version: AggregateVersion[E] = AggregateVersion.max): fs2.Stream[F, MetaEvent[E]] = {
    val z: fs2.Stream[F, MetaEvent[E]] = fs2.Stream.eval(Sync[F].delay(storage(id).take(version.version)))
      .flatMap { events =>
        fs2.Stream.apply[MetaEvent[E]](events: _*)
          .covary[F]
      }

    z
  }

  override def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit] = Sync[F].delay {
    val id = events.head.aggregateId
    storage = storage + (id -> storage.getOrElse(id, List()).++(events.toList))
  }
}
