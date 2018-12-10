package loco

import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import fs2.concurrent.Queue
import loco.IncrementFixture._
import loco.command.{Command, CommandResult}
import loco.repository.EventsRepository.ConcurrentModificationException
import loco.repository.InMemoryRepository
import loco.test.{ConsoleErrorReporter, ConsoleErrorReporterMatcher, FakeTimer}
import loco.view.View
import cats.implicits._

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class PartitionedEventSourceTest extends UnitSpec {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  trait ctx extends IncrementFixture with ConsoleErrorReporterMatcher[IO] {
    implicit val timer = IO.timer(ec)
    implicit val cs = IO.contextShift(ec)

    val fakeTimer: FakeTimer[IO] = FakeTimer[IO]()

    val repository = InMemoryRepository.unsafeCreate[IO, IncrementEvent]

    val errorReporter = ConsoleErrorReporter[IO]()
    val es: EventSourcing[IO, IncrementEvent, Increment] = DefaultEventSourcing[IO, IncrementEvent, Increment](IncrementAggregateBuilder, repository, View.empty[IO, IncrementEvent], errorReporter)(fakeTimer.clock, Sync[IO])

    val metaEvents = NonEmptyList.of(metaEvent1(fakeTimer.instant), metaEvent2(fakeTimer.instant))
  }


  "DefaultEventSourcing" should "throw an exception in case concurrent modification is occurred" in new ctx {

    val cmdWithDelay = new Command[IO, IncrementEvent, Increment, Unit] {
      override def events(a: Increment) = {
        for {
          _ <- cs.shift
          _ <- timer.sleep(10 seconds)
        } yield CommandResult.success(IncrementEvent())
      }
    }

    val cmd = new Command[IO, IncrementEvent, Increment, Unit] {
      override def events(a: Increment) = IO(CommandResult.success(IncrementEvent()))
    }

    val f = es.executeCommand(id, cmdWithDelay).unsafeToFuture()

    es.executeCommand(id, cmd).unsafeRunSync()


    assertThrows[ConcurrentModificationException[IncrementEvent]] {
      Await.result(f, 20.seconds)
    }
  }

  "PartitionedEventSourcing.partition0" should "queue all requests to perform every (write) action in a 'single thread' mode" in new ctx {
    val ess = PartitionedEventSource.partition0(es, Queue.unbounded[IO, IO[Unit]].unsafeRunSync()).unsafeRunSync()

    val cmdWithDelay = new Command[IO, IncrementEvent, Increment, Unit] {
      override def events(a: Increment) = {
        for {
          _ <- cs.shift
          _ <- timer.sleep(10 seconds)
        } yield CommandResult.success(IncrementEvent())
      }
    }

    val cmd = new Command[IO, IncrementEvent, Increment, Unit] {
      override def events(a: Increment) = IO(println("instant")) *> IO(CommandResult.success(IncrementEvent()))
    }


    val f = ess.executeCommand(id, cmdWithDelay).unsafeToFuture()

    ess.executeCommand(id, cmd).unsafeRunSync()


    Await.result(f, 20.seconds)

  }
}
