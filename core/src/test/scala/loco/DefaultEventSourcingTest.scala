package loco

import cats.data.NonEmptyList
import cats.effect.IO
import loco.IncrementFixture._
import loco.domain.{AggregateId, AggregateVersion}
import loco.repository.EventsRepository
import loco.test.{ConsoleErrorReporter, ConsoleErrorReporterMatcher, FakeTimer}
import loco.view.View

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

  it should "save events as expected via saveMetaEvents " in new ctx{
    (repository.saveEvents _).expects(metaEvents).returns(IO.unit)
    (view.handle _).expects(metaEvents).returns(IO.unit)

    noException should be thrownBy es.saveMetaEvents(metaEvents).unsafeRunSync()

    errorReporter shouldNot haveError
  }

  it should "save events as expected via saveMetaEvents with multiple ids" in new ctx{
    val ctx1 = new ctx{}
    (repository.saveEvents _).expects(ctx1.metaEvents).returns(IO.unit)
    (repository.saveEvents _).expects(metaEvents).returns(IO.unit)
    (view.handle _).expects(metaEvents).returns(IO.unit)
    (view.handle _).expects(ctx1.metaEvents).returns(IO.unit)

    noException should be thrownBy es.saveMetaEvents(metaEvents.concatNel(ctx1.metaEvents)).unsafeRunSync()

    errorReporter shouldNot haveError
  }

  it should "propagate error from repository" in new ctx {
    val error = new RuntimeException("error")
    (repository.saveEvents _).expects(metaEvents).returns(IO.raiseError(error))

    val exception = the[RuntimeException] thrownBy es.saveEvents(NonEmptyList.of(event1, event2), id, AggregateVersion.none).unsafeRunSync()

    assert(exception == error)
    errorReporter shouldNot haveError
  }
  it should "propagate error from repository in saveMetaEvents" in new ctx {
    val error = new RuntimeException("error")
    (repository.saveEvents _).expects(metaEvents).returns(IO.raiseError(error))

    val list = es.saveMetaEvents(metaEvents).unsafeRunSync()

    assert(list.head._2.contains(error))
    assert(list.length == 1)
    assert(list.head._1 == id)
    errorReporter shouldNot haveError
  }

  it should "propagate error from repository in saveMetaEvents with two ids" in new ctx {
    val ctx1 = new ctx {}
    val error = new RuntimeException("error")
    (repository.saveEvents _).expects(metaEvents).returns(IO.raiseError(error))
    (repository.saveEvents _).expects(ctx1.metaEvents).returns(IO.raiseError(error))

    val NonEmptyList(head,List(tail)) = es.saveMetaEvents(metaEvents.concatNel(ctx1.metaEvents)).unsafeRunSync()

    assert(head._1 == id)
    assert(head._2.contains(error))
    assert(tail._1 == ctx1.id)
    assert(tail._2.contains(error))
    errorReporter shouldNot haveError
  }

  it should "report error from view and ends successfully" in new ctx {
    val error = new RuntimeException("error")
    (repository.saveEvents _).expects(metaEvents).returns(IO.unit)
    (view.handle _).expects(metaEvents).returns(IO.raiseError(error))

    noException should be thrownBy es.saveEvents(NonEmptyList.of(event1, event2), id, AggregateVersion.none).unsafeRunSync()

    errorReporter should haveExactError(error)
  }

  it should "report error from view and ends successfully in save meta events" in new ctx {
    val error = new RuntimeException("error")
    (repository.saveEvents _).expects(metaEvents).returns(IO.unit)
    (view.handle _).expects(metaEvents).returns(IO.raiseError(error))

    val res: NonEmptyList[(AggregateId[IncrementEvent], Option[Throwable])] = es.saveMetaEvents(metaEvents).unsafeRunSync()

    assert(res.length == 1)
    assert(res.head._2.isEmpty)
    errorReporter should haveExactError(error)
  }

  it should "fetch some meta aggregate" in new ctx {
    (repository.fetchEvents _).expects(id, AggregateVersion.max).returns( fs2.Stream(metaEvents.toList : _*))

    es.fetchMetaAggregate(id).unsafeRunSync() should be(Some(aggregate))
  }

  it should "fetch none aggregate" in new ctx {
    (repository.fetchEvents _).expects(id, AggregateVersion.max).returns(fs2.Stream.empty.covary[IO])

    es.fetchMetaAggregate(id).unsafeRunSync() should be(None)
  }

}
