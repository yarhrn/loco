package loco.domain

import java.time.Instant
import java.util.UUID

import loco.AggregateBuilder
import org.scalatest.{FlatSpec, Matchers}

class MetaAggregateBuilderSpec extends FlatSpec with Matchers {

  trait ctx {

    case class IncrementEvent(id: String = UUID.randomUUID().toString) extends Event

    case class Increment(id: AggregateId[IncrementEvent], count: Int, events: List[IncrementEvent]) extends Aggregate[IncrementEvent]

    object Increment extends AggregateBuilder[Increment, IncrementEvent] {
      override def empty(id: AggregateId[IncrementEvent]) = Increment(id, 0, List.empty)

      override def apply(aggregate: Increment, metaEvent: MetaEvent[IncrementEvent]) = {
        aggregate.copy(count = aggregate.count + 1, events = aggregate.events :+ metaEvent.domainEvent)
      }
    }


    val metaAggregateBuilder = MetaAggregateBuilder(Increment)
    val id = AggregateId[IncrementEvent](UUID.randomUUID().toString)
    val instant = Instant.now()
  }

  "MetaAggregateBuilder" should "properly create empty state with zero version" in new ctx {
    metaAggregateBuilder.empty(id) shouldBe MetaAggregate[IncrementEvent, Increment](Increment(id, 0, List()), AggregateVersion[IncrementEvent](0))
  }

  it should "properly build meta aggregate  " in new ctx {
    val emptyMetaAggregate = metaAggregateBuilder.empty(id)
    val event = IncrementEvent ()
    val metaAggregate = metaAggregateBuilder.apply(emptyMetaAggregate, MetaEvent(id, event, instant, AggregateVersion(10)))
    metaAggregate shouldBe MetaAggregate[IncrementEvent,Increment](Increment(id,1,List(event)),AggregateVersion(10))
  }
}