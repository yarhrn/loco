package loco.repository

import java.time.Instant

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import loco.domain._
import org.scalatest.{FunSuite, Matchers}

class InMemoryRepositoryTest extends FunSuite with Matchers with TestDomainData {

  type Forum[A] = EitherT[IO, Throwable, A]

  val repository = new InMemoryRepository[Forum, ForumPostEvent]()

  test("Should store events") {

    val metaEvent = getMetaEvent(AggregateVersion(1), "Hello world", Users.john)

    val result = repository.saveEvents(NonEmptyList.one(metaEvent))

    val either = result.value.unsafeRunSync()
    either.isLeft shouldBe false
    either.isRight shouldBe true
  }

  def getMetaEvent(version: AggregateVersion[ForumPostEvent], content: String, author: User):MetaEvent[ForumPostEvent] = {
    val postCreated = ForumPostEvents.PostCreated(nextVal(), content, author)
    MetaEvent[ForumPostEvent](postCreated.id, postCreated, postCreated.created, version)
  }
}
