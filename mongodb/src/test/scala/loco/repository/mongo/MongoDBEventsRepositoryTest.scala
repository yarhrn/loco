package loco.repository.mongo


import cats.data.NonEmptyList
import cats.effect.IO
import loco.IncrementFixture._
import loco.domain.{AggregateVersion, MetaEvent}
import loco.test.FakeTimer
import loco.{IncrementFixture, UnitSpec}
import loco.repository.EmbeddedDBEnv._

class MongoDBEventsRepositoryTest extends UnitSpec {


  trait ctx extends IncrementFixture {
    val repository = new MongoDBEventsRepository[IO, IncrementEvent](collection)
    val timer = FakeTimer[IO]()
  }

  "Mongodb events repository" should "save events and retrieve events" in new ctx {

    val metaEvents: NonEmptyList[MetaEvent[IncrementEvent]] = NonEmptyList.fromListUnsafe(
      List.tabulate(10)(counter => metaEventFrom(newEvent, timer.tick().instant, counter + 1))
    )

    repository.saveEvents(metaEvents).unsafeRunSync()

    repository.fetchEvents(id, AggregateVersion.max).compile.to[List].unsafeRunSync() shouldBe metaEvents.toList
  }


}
