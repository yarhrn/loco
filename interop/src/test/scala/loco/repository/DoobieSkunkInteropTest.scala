package loco.repository

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.util.transactor.Transactor
import loco.IncrementFixture._
import loco._
import loco.domain.{AggregateVersion, MetaEvent}
import loco.repository.persistent.Codec
import loco.repository.persistent.doobie.{DoobieEventsRepository, EventsTableConfiguration => DoobieTableConfiguration}
import loco.repository.persistent.skunk.{EventsTableConfiguration => SkunkTableConfiguration, SkunkEventsRepository}
import loco.test.FakeTimer
import _root_.skunk.Session
import org.typelevel.otel4s.trace.Tracer

import scala.concurrent.ExecutionContext

class DoobieSkunkInteropTest extends UnitSpec with EmbeddedPosrtesqlDBEnv {

  val doobieConfiguration = DoobieTableConfiguration.base("increment")
  val skunkConfiguration = SkunkTableConfiguration.base("increment")

  trait ctx extends IncrementFixture {
    implicit val tracer: Tracer[IO] = Tracer.noop[IO]
    val (events, logHandler) = RecordingLogHandler.logHandler
    private val executor = ExecutionContext.fromExecutor(_.run())

    val transactor = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      postgres.jdbcUrl,
      postgres.username,
      postgres.password,
      Some(logHandler)
    )

    val session = Session.single[IO](
      host = postgres.container.getHost,
      port = postgres.container.getMappedPort(5432),
      user = postgres.username,
      database = postgres.databaseName,
      password = Some(postgres.password)
    )

    val codec = Codec.fromJsonCodec(IncrementFixture.jsonValueCodec)

    val doobieRepository = DoobieEventsRepository[IO, IncrementEvent](
      codec,
      transactor,
      batchSize = 1,
      tableConfiguration = doobieConfiguration)

    val skunkRepository = SkunkEventsRepository[IO, IncrementEvent](
      codec,
      session,
      batchSize = 1,
      tableConfiguration = skunkConfiguration)

    val timer = FakeTimer[IO]()

    def buildMetaEvents: NonEmptyList[MetaEvent[IncrementEvent]] = NonEmptyList.fromListUnsafe(
      List.tabulate(10)(counter => metaEventFrom(newEvent, timer.tick().instant, counter + 1))
    )
  }

  "Doobie and Skunk repositories" should "be interchangeable: doobie writes, skunk reads" in new ctx {

    import cats.effect.unsafe.implicits.global

    val metaEvents = buildMetaEvents

    doobieRepository.saveEvents(metaEvents).unsafeRunSync()

    skunkRepository.fetchEvents(id, AggregateVersion.max).compile.toList.unsafeRunSync() shouldBe metaEvents.toList
  }

  it should "be interchangeable: skunk writes, doobie reads" in new ctx {

    import cats.effect.unsafe.implicits.global

    val metaEvents = buildMetaEvents

    skunkRepository.saveEvents(metaEvents).unsafeRunSync()

    doobieRepository.fetchEvents(id, AggregateVersion.max).compile.toList.unsafeRunSync() shouldBe metaEvents.toList
  }

}
