package loco.repository

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import loco.repository.EventsRepository.ConcurrentModificationException

import scala.language.higherKinds

case class InMemoryRepository[F[_] : Sync, E <: Event](storage: Ref[F, Map[AggregateId[E], List[MetaEvent[E]]]]) extends EventsRepository[F, E] {

  override def fetchEvents(id: AggregateId[E], version: AggregateVersion[E] = AggregateVersion.max): fs2.Stream[F, MetaEvent[E]] = {
    fs2.Stream.eval(storage.get.map(m => m(id).take(version.version)))
      .flatMap { events =>
        fs2.Stream.apply(events: _*)
      }
  }

  override def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit] = {
    storage.update {
      old =>
        val id = events.head.aggregateId
        val eventsList = events.toList
        old.get(id) match {
          case None => old + (id -> eventsList)
          case Some(x) =>
            if (events.map(_.version).toList.toSet.intersect(x.map(_.version).toSet).isEmpty) {
              old + (id -> (x ++ eventsList))
            } else {
              throw new ConcurrentModificationException(id, eventsList.map(_.version))
            }
        }
    }
  }
}

object InMemoryRepository {
  def unsafeCreate[F[_] : Sync, E <: Event] = {
    val storage = Ref.unsafe(Map.empty[AggregateId[E], List[MetaEvent[E]]])
    InMemoryRepository(storage)
  }
}