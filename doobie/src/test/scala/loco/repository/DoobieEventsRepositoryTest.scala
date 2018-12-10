package loco.repository

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.util.transactor.Transactor
import loco.EmbeddedDBEnv._
import loco.IncrementFixture._
import loco._
import loco.domain.{AggregateVersion, MetaEvent}
import loco.repository.persistent.Codec
import loco.repository.persistent.doobie.{DoobieEventsRepository, EventsTableConfiguration}
import loco.test.FakeTimer

import scala.concurrent.ExecutionContext

class DoobieEventsRepositoryTest extends UnitSpec with ITTest {

  val configuration = EventsTableConfiguration.base("increment")

  def schemaScript = configuration.setup

  trait ctx extends IncrementFixture {
    val (events, logHandler) = RecordingLogHandler.logHandler
    private val executor = ExecutionContext.fromExecutor(_.run())
    implicit val cs = IO.contextShift(executor)
    val transactor = Transactor.fromDriverManager[IO](
      "com.mysql.cj.jdbc.Driver",
      s"jdbc:mysql://localhost:$port/$schema",
      username,
      password)


    val codec = Codec.fromJsonCodec(IncrementFixture.jsonValueCodec)
    val repository = DoobieEventsRepository[IO, IncrementEvent](codec, transactor, logHandler, batchSize = 1, tableConfiguration = configuration)
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
