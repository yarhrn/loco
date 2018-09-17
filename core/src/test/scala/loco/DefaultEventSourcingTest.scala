package loco

import cats.data.NonEmptyList
import cats.effect.IO
import loco.domain.{AggregateVersion, MetaEvent}
import loco.repository.EventsRepository
import loco.test.{ConsoleErrorReporter, ConsoleErrorReporterMatcher, FakeTimer}
import loco.view.View
import loco.IncrementFixture._

class DefaultEventSourcingTest extends UnitSpec {

  trait ctx extends IncrementFixture with ConsoleErrorReporterMatcher[IO] {

    implicit val timer: FakeTimer[IO] = FakeTimer[IO]()

    val repository = mock[EventsRepository[IO, IncrementEvent]]

    val view = mock[View[IO, IncrementEvent]]
    val errorReporter = ConsoleErrorReporter[IO]()
    val es: EventSourcing[IO, IncrementEvent, Increment] = DefaultEventSourcing[IO, IncrementEvent, Increment](IncrementAggregateBuilder, repository, view, errorReporter)

    val metaEvents = NonEmptyList.of(metaEvent1(timer.instant), metaEvent2(timer.instant))
  }

  "DefaultEventSourcing" should "save events as expected" in new ctx {
    (repository.saveEvents _).expects(metaEvents).returns(IO.unit)
    (view.handle _).expects(metaEvents).returns(IO.unit)

    noException should be thrownBy es.saveEvents(NonEmptyList.of(event1, event2), id, AggregateVersion.none).unsafeRunSync()

    errorReporter shouldNot haveError
  }

  it should "propagate error from repository" in new ctx {
    val error = new RuntimeException("error")
    (repository.saveEvents _).expects(metaEvents).returns(IO.raiseError(error))

    val exception = the[RuntimeException] thrownBy es.saveEvents(NonEmptyList.of(event1, event2), id, AggregateVersion.none).unsafeRunSync()

    assert(exception == error)
    errorReporter shouldNot haveError
  }

  it should "report error from view and ends successfully" in new ctx {
    val error = new RuntimeException("error")
    (repository.saveEvents _).expects(metaEvents).returns(IO.unit)
    (view.handle _).expects(metaEvents).returns(IO.raiseError(error))

    noException should be thrownBy es.saveEvents(NonEmptyList.of(event1, event2), id, AggregateVersion.none).unsafeRunSync()

    errorReporter should haveExactError(error)
  }

  it should "fetch some meta aggregate" in new ctx {
    (repository.fetchEvents _).expects(id, AggregateVersion.max).returns(fs2.Stream.fromIterator[IO, MetaEvent[IncrementEvent]](metaEvents.toList.iterator))

    es.fetchMetaAggregate(id).unsafeRunSync() should be(Some(aggregate))
  }

  it should "fetch none aggregate" in new ctx {
    (repository.fetchEvents _).expects(id, AggregateVersion.max).returns(fs2.Stream.empty.covary[IO])

    es.fetchMetaAggregate(id).unsafeRunSync() should be(None)
  }

}
