package loco

import java.time.Instant
import java.util.Currency
import cats.effect.unsafe.implicits.global
import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Clock
import loco.domain.{AggregateId, AggregateVersion, MetaEvent}
import loco.repository._
import loco.test.{ConsoleErrorReporter, ConsoleErrorReporterMatcher, FakeTimer}
import loco.view.View
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ExampleSpec extends AnyFlatSpec with Matchers with MockFactory {

  trait EventSourcingContext {
    val mockedView = stub[View[IO, TransactionEvent]]
    val timer: FakeTimer[IO] = new FakeTimer[IO]()
    implicit val clock: Clock[IO] = timer.clock
    val errorReporter: ConsoleErrorReporter[IO] = new ConsoleErrorReporter[IO]()
    val eventRepository = InMemoryRepository.unsafeCreate[IO, TransactionEvent]
    val eventSourcing = DefaultEventSourcing[IO, TransactionEvent, Transaction](TransactionBuilder, eventRepository, mockedView, errorReporter)
  }

  trait TransactionContext {
    val eventTransactionCreated = TransactionCreated(10, Currency.getInstance("BRL"), "provider-account-id-1")

    def aggregateTransactionCreated(id: AggregateId[TransactionEvent]) = Transaction(id, TransactionStatus.New, 10, Currency.getInstance("BRL"), "provider-account-id-1", None, None)

    def metaEventTransactionCreated(id: AggregateId[TransactionEvent], instant: Instant) = MetaEvent[TransactionEvent](id, eventTransactionCreated, instant, AggregateVersion(1))

  }


  "A EventSourcing" should "generate id and save meta events with proper version and invoke view" in new EventSourcingContext with TransactionContext with ConsoleErrorReporterMatcher[IO] {
    (mockedView.handle _).when(*).returns(IO.unit)

    val id = eventSourcing.saveEvents(NonEmptyList.of(eventTransactionCreated)).unsafeRunSync()

    val metaEvents = eventRepository.fetchEvents(id).compile.toList.unsafeRunSync()
    metaEvents should have size 1
    metaEvents.head shouldEqual metaEventTransactionCreated(id, timer.instant)

    errorReporter shouldNot haveError
    assert(eventSourcing.fetchMetaAggregate(id).map(_.get.aggregate).unsafeRunSync() == aggregateTransactionCreated(id))

    (mockedView.handle _).verify(NonEmptyList.of(metaEventTransactionCreated(id, timer.instant)))
  }

}