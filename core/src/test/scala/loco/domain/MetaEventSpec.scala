package loco.domain

import java.time.Instant
import cats.data.NonEmptyList
import loco.UnitSpec

class MetaEventSpec extends UnitSpec {

  trait ctx {

    case class DumbEvent(discriminator: Int) extends Event

    val event: NonEmptyList[DumbEvent] = NonEmptyList.of(DumbEvent(1))
    val events: NonEmptyList[DumbEvent] = NonEmptyList.fromListUnsafe((1 to 3).map(DumbEvent).toList)

    val id = AggregateId[DumbEvent]("test")

    val version0 = AggregateVersion[DumbEvent](0)
    val version10 = AggregateVersion[DumbEvent](10)
    val instant = Instant.now()

    val metaEventsFromVersion0 = NonEmptyList.of(
      MetaEvent(id, events.toList(0), instant, AggregateVersion(1)),
      MetaEvent(id, events.toList(1), instant, AggregateVersion(2)),
      MetaEvent(id, events.toList(2), instant, AggregateVersion(3)))

    val metaEventsFromVersion10 = NonEmptyList.of(
      MetaEvent(id, events.toList(0), instant, AggregateVersion(11)),
      MetaEvent(id, events.toList(1), instant, AggregateVersion(12)),
      MetaEvent(id, events.toList(2), instant, AggregateVersion(13)))
  }

  "MetaEvent" should "proper construct list of meta events from one event" in new ctx {
    val metaEvents: Seq[MetaEvent[DumbEvent]] = MetaEvent.fromRawEvents(id, instant, version0, event).toList

    metaEvents should have size 1
    metaEvents.head.aggregateId shouldBe id
    metaEvents.head.version shouldBe AggregateVersion[DumbEvent](1)
    metaEvents.head.createdAt shouldBe instant
    metaEvents.head.domainEvent shouldBe event.head
  }

  it should "proper calculate version of list of events from version 0" in new ctx {
    MetaEvent.fromRawEvents(id, instant, version0, events) shouldBe metaEventsFromVersion0
  }

  it should "proper calculate version of list of events from version 10" in new ctx {
    MetaEvent.fromRawEvents(id, instant, version10, events) shouldBe metaEventsFromVersion10
  }

}