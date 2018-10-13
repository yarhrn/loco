package loco

import cats.data.NonEmptyList
import cats.effect.Concurrent
import cats.effect.concurrent.Deferred
import cats.implicits._
import fs2.concurrent.Queue
import loco.command.Command
import loco.domain.{Aggregate, AggregateId, AggregateVersion, Event}

object PartitionedEventSource {

  def partition[F[_] : Concurrent, E <: Event, A <: Aggregate[E]](es: EventSourcing[F, E, A], queue: Queue[F, F[Unit]]) = {


    val ess = new EventSourcing[F, E, A] {

      override def saveEvents(events: NonEmptyList[E], id: AggregateId[E], lastKnownVersion: AggregateVersion[E]) = {

        for {
          d <- Deferred[F, Either[Throwable, AggregateId[E]]]
          _ <- queue.enqueue1 {
            es.saveEvents(events, id, lastKnownVersion)
              .attempt
              .flatMap(id => d.complete(id))
          }
          id <- d.get.rethrow
        } yield id
      }

      override def executeCommand[R](id: AggregateId[E], command: Command[F, E, A, R]) = {
        for {
          d <- Deferred[F, Either[Throwable, R]]
          _ <- queue.enqueue1 {
            es.executeCommand(id, command)
              .attempt
              .flatMap(r => d.complete(r))
          }
          r <- d.get.rethrow
        } yield r
      }

      override def fetchMetaAggregate(id: AggregateId[E]) = es.fetchMetaAggregate(id)
    }

    for{
      _ <- queue.dequeue.flatMap{
        action => fs2.Stream.eval(action)
      }.compile.drain
    } yield ess
  }
}
