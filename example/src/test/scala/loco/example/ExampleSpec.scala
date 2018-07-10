package loco.example

import java.time.Instant
import java.util.Currency
import java.util.concurrent.TimeUnit

import cats.data.NonEmptyList
import cats.effect.{IO, Timer}
import loco.domain.{AggregateVersion, MetaEvent}
import loco.repository._
import loco.{ES, ErrorReporter}
import org.scalatest._

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

class ExampleSpec extends FlatSpec with Matchers {

  trait EventSourcingContext {
    var currentTimeMillis = System.currentTimeMillis()

    def tick = currentTimeMillis + 1000

    implicit val timer = new Timer[IO] {
      override def clockRealTime(unit: TimeUnit): IO[Long] = IO(TimeUnit.MILLISECONDS.convert(currentTimeMillis, unit))

      override def clockMonotonic(unit: TimeUnit): IO[Long] = clockRealTime(unit)

      override def sleep(duration: FiniteDuration): IO[Unit] = IO.unit

      override def shift: IO[Unit] = IO.unit
    }
    val errorReporter = new ErrorReporter[IO] {
      override def error(throwable: Throwable): IO[Unit] = IO(throwable.printStackTrace())
    }
    val eventRepository = InMemoryRepository[IO, TransactionEvent]()
    val eventSourcing = new ES[IO, TransactionEvent, Transaction](TransactionBuilder, eventRepository, List(), List(), List(), errorReporter)
  }


  "A EventSourcing" should "generate id and save meta events with proper version" in new EventSourcingContext {

    val event = TransactionCreated(10, Currency.getInstance("BRL"), "provider-account-id-1")

    val id = eventSourcing.saveEvents(NonEmptyList.of(event)).unsafeRunSync()

    val metaEvents = eventRepository.fetchEvents(id).toListL.unsafeRunSync()
    metaEvents should have size 1
    metaEvents.head shouldEqual MetaEvent(id, event, Instant.ofEpochMilli(currentTimeMillis), AggregateVersion(1))

    assert(eventSourcing.fetchMetaAggregate(id).map(_.get.aggregate).unsafeRunSync() == Transaction(id, TransactionStatus.New, 10, Currency.getInstance("BRL"), "provider-account-id-1", None, None))
  }

}