package loco.repository

import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import cats.effect.concurrent.Ref
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import cats.implicits._
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
        old.get(id) match {
          case None => old + (id -> events.toList)
          case Some(x) =>
            if (events.map(_.version).toList.toSet.intersect(x.map(_.version).toList.toSet).isEmpty) {
              old + (id -> (x ++ events.toList))
            } else {
              throw new ConcurrentModificationException()
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


  object Foo extends Event

  def main(args: Array[String]): Unit = {
    val unit = unsafeCreate[IO, Foo.type]
    val asd = MetaEvent(
      AggregateId("1"),
      Foo,
      Instant.now(),
      AggregateVersion(1)
    )
    unit.saveEvents(NonEmptyList.of(asd)).unsafeRunSync()
    unit.saveEvents(NonEmptyList.of(asd.copy(version = AggregateVersion(2)))).unsafeRunSync()
  }
}