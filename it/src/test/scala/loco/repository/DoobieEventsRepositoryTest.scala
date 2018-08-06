package loco.repository

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.util.transactor.Transactor
import loco.EmbeddedDBEnv._
import loco.domain.{AggregateVersion, MetaEvent}
import loco.test.FakeTimer
import loco.{IncrementFixture, RecordingLogHandler, UnitSpec}

class DoobieEventsRepositoryTest extends UnitSpec {

  trait ctx extends IncrementFixture {
    val (events, logHandler) = RecordingLogHandler.logHandler
    val transactor = Transactor.fromDriverManager[IO](
      "com.mysql.cj.jdbc.Driver",
      s"jdbc:mysql://localhost:$port/$schema",
      username,
      password)

    val codec = new Codec[IncrementEvent] {
      override def encode(e: IncrementEvent) = e.id

      override def decode(e: String) = IncrementEvent(e)
    }
    val repository = DoobieEventsRepository[IO, IncrementEvent](codec, transactor, "increment_events", logHandler, batchSize = 1)
    val timer = FakeTimer[IO]()
  }

  "Doobie events repository" should "save events and retrieve events" in new ctx {

    val metaEvents: NonEmptyList[MetaEvent[IncrementEvent]] = NonEmptyList.fromListUnsafe(
      List.tabulate(10)(counter => metaEventFrom(newEvent, timer.tick().instant, counter + 1))
    )

    repository.saveEvents(metaEvents).unsafeRunSync()

    repository.fetchEvents(id, AggregateVersion.max).compile.to[List].unsafeRunSync() shouldBe metaEvents.toList
    assert(events.events.size == 6, "size should be 6")
  }


}
