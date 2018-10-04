package loco.repository.persistent.mongo


import java.util.UUID
import java.util.concurrent.Executors

import cats.data.NonEmptyList
import cats.effect.IO
import loco.IncrementFixture._
import loco.domain.{AggregateVersion, MetaEvent}
import loco.repository.EmbeddedDBEnv._
import loco.repository.ITTest
import loco.test.FakeTimer
import loco.{IncrementFixture, UnitSpec}

import scala.concurrent.ExecutionContext

class MongoDBEventsRepositoryTest extends UnitSpec with ITTest {


  trait ctx extends IncrementFixture {
    var threadName = UUID.randomUUID().toString
    val pool = Executors.newFixedThreadPool(1, (r: Runnable) => {
      val trd = new Thread(r)
      trd.setName(threadName)
      trd
    })
    val repository = new MongoDBEventsRepository[IO, IncrementEvent](collection, IO.contextShift(ExecutionContext.fromExecutor(pool)))
    val timer = FakeTimer[IO]()
  }

  "Mongodb events repository" should "save events and retrieve events" in new ctx {

    val metaEvents: NonEmptyList[MetaEvent[IncrementEvent]] = NonEmptyList.fromListUnsafe(
      List.tabulate(10)(counter => metaEventFrom(newEvent, timer.tick().instant, counter + 1))
    )

    repository.saveEvents(metaEvents).map { _ =>
      Thread.currentThread().getName should be(threadName)
    }.unsafeRunSync()

    repository.fetchEvents(id, AggregateVersion.max)
      .map{ a =>
        Thread.currentThread().getName should be(threadName)
        a
      }.compile.to[List].unsafeRunSync() shouldBe metaEvents.toList
  }


}