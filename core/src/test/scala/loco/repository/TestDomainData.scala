package loco.repository

import java.time.Instant
import java.util.UUID

import loco.domain._
trait TestDomainData {

  sealed trait Role
  case object Author extends Role
  case object Admin extends Role

  case class User(login: String, role: Role)
  case class ForumPost(content: String, created: Instant, author: User, version: AggregateVersion[ForumPostEvent])
  type ForumPostId = AggregateId[ForumPostEvent]

  def nextVal(): ForumPostId = AggregateId[ForumPostEvent](UUID.randomUUID().toString)

  sealed trait ForumPostEvent extends Event {
    val id: ForumPostId
    val author: User
    val created: Instant
  }

  object ForumPostEvents {
    case class PostCreated(id: ForumPostId, content: String, author: User, created: Instant = Instant.now())
        extends ForumPostEvent
    case class PostUpdated(id: ForumPostId, content: String, author: User, created: Instant = Instant.now())
        extends ForumPostEvent
    case class PostDeleted(id: ForumPostId, author: User, created: Instant = Instant.now()) extends ForumPostEvent
  }

  object Users {
    val john = User("john@loco.io", Author)
    val smith = User("smith@loco.io", Admin)
  }
}
