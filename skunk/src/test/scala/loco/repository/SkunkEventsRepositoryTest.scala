package loco.repository

import cats.data.NonEmptyList
import cats.effect.IO
import loco.IncrementFixture._
import loco._
import loco.domain.{AggregateVersion, MetaEvent}
import loco.repository.persistent.Codec
import loco.repository.persistent.skunk.{EventsTableConfiguration, SkunkEventsRepository}
import loco.test.FakeTimer
import _root_.skunk.Session
import org.typelevel.otel4s.trace.Tracer

class SkunkEventsRepositoryTest extends UnitSpec with EmbeddedPosrtesqlDBEnv {

  val configuration = EventsTableConfiguration.base("increment")

  def schemaScript = configuration.setup

  trait ctx extends IncrementFixture {
    implicit val tracer: Tracer[IO] = Tracer.noop[IO]
    val session = Session.single[IO](
      host = postgres.container.getHost,
      port = postgres.container.getMappedPort(5432),
      user = postgres.username,
      database = postgres.databaseName,
      password = Some(postgres.password)
    )

    val codec = Codec.fromJsonCodec(IncrementFixture.jsonValueCodec)
    val repository = SkunkEventsRepository[IO, IncrementEvent](
      codec,
      session,
      batchSize = 1,
      tableConfiguration = configuration)
    val timer = FakeTimer[IO]()
  }

  "Skunk events repository" should "save events and retrieve events" in new ctx {

    import cats.effect.unsafe.implicits.global

    val metaEvents: NonEmptyList[MetaEvent[IncrementEvent]] = NonEmptyList.fromListUnsafe(
      List.tabulate(10)(counter => metaEventFrom(newEvent, timer.tick().instant, counter + 1))
    )

    repository.saveEvents(metaEvents).unsafeRunSync()

    repository.fetchEvents(id, AggregateVersion.max).compile.toList.unsafeRunSync() shouldBe metaEvents.toList
  }

}
