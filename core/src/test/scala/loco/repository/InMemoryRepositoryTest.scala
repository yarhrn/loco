package loco.repository

import cats.data.NonEmptyList
import cats.effect.IO
import loco.domain._
import org.scalatest.{FlatSpec, Matchers}

class InMemoryRepositoryTest extends FlatSpec with Matchers with TestDomainData {

  trait ctx {
    val repository = new InMemoryRepository[Forum, ForumPostEvent]()
  }

  type Forum[A] = IO[A]

  "InMemoryRepository" should "store events" in new ctx {

    private val metaEvent = getMetaEvent(AggregateVersion(1), "Hello world", Users.john)

    private val result = repository.saveEvents(NonEmptyList.one(metaEvent))

    private val either: Unit = result.unsafeRunSync()
    either.leftSide shouldBe ()
  }

  def getMetaEvent(version: AggregateVersion[ForumPostEvent], content: String, author: User):MetaEvent[ForumPostEvent] = {
    val postCreated = ForumPostEvents.PostCreated(nextVal(), content, author)
    MetaEvent[ForumPostEvent](postCreated.id, postCreated, postCreated.created, version)
  }
}
