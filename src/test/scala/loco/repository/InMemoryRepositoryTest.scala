package loco.repository

import cats.data.NonEmptyList
import cats.effect.IO
import loco.domain._
import cats.implicits._
import loco.IncrementFixture.IncrementEvent
import loco.repository.EventsRepository.ConcurrentModificationException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InMemoryRepositoryTest extends AnyFlatSpec with Matchers with TestDomainData {

  trait ctx {
    val repository = InMemoryRepository.unsafeCreate[IO, ForumPostEvent]
  }

  "InMemoryRepository" should "store events" in new ctx {
    import cats.effect.unsafe.implicits.global
    val metaEvent = getMetaEvent(AggregateVersion(1), "Hello world", Users.john)

    val result = repository.saveEvents(NonEmptyList.one(metaEvent)).flatMap(_ => repository.fetchEvents(metaEvent.aggregateId).compile.toList)

    result.unsafeRunSync().head shouldBe metaEvent
  }

  it should "throw exception in case of same version is saved" in new ctx {
    import cats.effect.unsafe.implicits.global
    val metaEvent = getMetaEvent(AggregateVersion(1), "Hello world", Users.john)

    val savingEvents = repository.saveEvents(NonEmptyList.one(metaEvent))
    assertThrows[ConcurrentModificationException[IncrementEvent]] {
      (savingEvents, savingEvents).tupled.unsafeRunSync()
    }


    repository.fetchEvents(metaEvent.aggregateId).compile.toList.unsafeRunSync().head shouldBe metaEvent
  }

  it should "not throw exception but return empty list" in new ctx{
    import cats.effect.unsafe.implicits.global
    repository.fetchEvents(AggregateId.random).compile.toList.unsafeRunSync() shouldBe List()
  }

  def getMetaEvent(version: AggregateVersion[ForumPostEvent], content: String, author: User): MetaEvent[ForumPostEvent] = {
    val postCreated = ForumPostEvents.PostCreated(nextVal(), content, author)
    MetaEvent[ForumPostEvent](postCreated.id, postCreated, postCreated.created, version)
  }
}
