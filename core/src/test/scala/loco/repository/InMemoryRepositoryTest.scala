package loco.repository

import java.time.Instant

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import loco.domain._
import org.scalatest.{FlatSpec, Matchers}

class InMemoryRepositoryTest extends FlatSpec with Matchers with TestDomainData {

  trait ctx {
    val repository = new InMemoryRepository[Forum, ForumPostEvent]()
  }

  type Forum[A] = IO[A]

  "InMemoryRepository" should "store events" in new ctx {

    val metaEvent = getMetaEvent(AggregateVersion(1), "Hello world", Users.john)

    val result = repository.saveEvents(NonEmptyList.one(metaEvent))

    val either = result.unsafeRunSync()
    either.leftSide shouldBe ()
  }

  def getMetaEvent(version: AggregateVersion[ForumPostEvent], content: String, author: User):MetaEvent[ForumPostEvent] = {
    val postCreated = ForumPostEvents.PostCreated(nextVal(), content, author)
    MetaEvent[ForumPostEvent](postCreated.id, postCreated, postCreated.created, version)
  }
}
