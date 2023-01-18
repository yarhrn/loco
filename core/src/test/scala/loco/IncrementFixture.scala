package loco

import java.time.Instant
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import loco.domain._

trait IncrementFixture {

  import IncrementFixture._

  val id = AggregateId[IncrementEvent](UUID.randomUUID().toString)

  object IncrementAggregateBuilder extends AggregateBuilder[Increment, IncrementEvent] {
    override def empty(id: AggregateId[IncrementEvent]) = Increment(id, 0, List.empty)

    override def apply(aggregate: Increment, metaEvent: MetaEvent[IncrementEvent]) = {
      aggregate.copy(count = aggregate.count + 1, events = aggregate.events :+ metaEvent.event)
    }
  }

  val event1 = IncrementEvent()
  val event2 = IncrementEvent()

  def newEvent = IncrementEvent()

  def metaEventFrom(event: IncrementEvent, createdAt: Instant, version: Int) =
    MetaEvent[IncrementEvent](id, event, createdAt, AggregateVersion[IncrementEvent](version))

  def metaEvent1(createdAt: Instant) =
    MetaEvent[IncrementEvent](id, event1, createdAt, AggregateVersion[IncrementEvent](1))

  def metaEvent2(createdAt: Instant) =
    MetaEvent[IncrementEvent](id, event2, createdAt, AggregateVersion[IncrementEvent](2))

  val aggregate = MetaAggregate[IncrementEvent, Increment](Increment(id, 2, List(event1, event2)), AggregateVersion(2))
}

object IncrementFixture {

  case class IncrementEvent(id: String = UUID.randomUUID().toString) extends Event

  case class Increment(id: AggregateId[IncrementEvent], count: Int, events: List[IncrementEvent])
      extends Aggregate[IncrementEvent]

  val jsonValueCodec = JsonCodecMaker.make[IncrementEvent](CodecMakerConfig)
}
