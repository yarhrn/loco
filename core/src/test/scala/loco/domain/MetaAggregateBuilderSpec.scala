package loco.domain

import java.time.Instant
import loco.{IncrementFixture, UnitSpec}
import loco.IncrementFixture._

class MetaAggregateBuilderSpec extends UnitSpec {

  trait ctx extends IncrementFixture {
    val metaAggregateBuilder = MetaAggregateBuilder(IncrementAggregateBuilder)
    val instant = Instant.now()
  }

  "MetaAggregateBuilder" should "properly create empty state with zero version" in new ctx {
    metaAggregateBuilder.empty(id) shouldBe MetaAggregate[IncrementEvent, Increment](Increment(id, 0, List()), AggregateVersion[IncrementEvent](0))
  }

  it should "properly build meta aggregate  " in new ctx {
    val emptyMetaAggregate = metaAggregateBuilder.empty(id)
    val event = IncrementEvent()
    val metaAggregate = metaAggregateBuilder.apply(emptyMetaAggregate, MetaEvent(id, event, instant, AggregateVersion(10)))
    metaAggregate shouldBe MetaAggregate[IncrementEvent, Increment](Increment(id, 1, List(event)), AggregateVersion(10))
  }
}