package loco

import cats.data.NonEmptyList
import cats.effect.Concurrent
import cats.effect.concurrent.Deferred
import cats.implicits._
import fs2.concurrent.Queue
import loco.command.Command
import loco.domain.{Aggregate, AggregateId, AggregateVersion, Event, MetaEvent}

object PartitionedEventSource {

  def partition0[F[_] : Concurrent, E <: Event, A <: Aggregate[E]](es: EventSourcing[F, E, A], queue: Queue[F, F[Unit]]): F[EventSourcing[F, E, A]] = {


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

      /**
       * Saves the given `meta events`.
       * Should fail in case an event with some id and version is already exist.
       */
      override def saveMetaEvents(events: NonEmptyList[MetaEvent[E]]): F[NonEmptyList[(AggregateId[E], Option[Throwable])]] = {
        for {
          d <- Deferred[F, Either[Throwable, NonEmptyList[(AggregateId[E], Option[Throwable])]]]
          _ <- queue.enqueue1 {
            es.saveMetaEvents(events)
              .attempt
              .flatMap(id => d.complete(id))
          }
          id <- d.get.rethrow
        } yield id
      }
    }

    for {
      _ <- Concurrent[F].start(queue.dequeue.flatMap {
        action => fs2.Stream.eval(action)
      }.compile.drain)
    } yield ess
  }

  def partition[F[_] : Concurrent, E <: Event, A <: Aggregate[E]](es: EventSourcing[F, E, A], partitionNumber: Int, queue: F[Queue[F, F[Unit]]]) = {
    for {
      queues <-  queue.replicateA(partitionNumber)
      partitions <- queues.traverse(partition0[F, E, A](es, _)).map(_.zipWithIndex.map(_.swap).toMap)
    } yield new EventSourcing[F, E, A] {

      override def saveEvents(events: NonEmptyList[E], id: AggregateId[E], lastKnownVersion: AggregateVersion[E]) = {
        partitions(id.hashCode() % partitionNumber)
          .saveEvents(events, id, lastKnownVersion)
      }


      override def executeCommand[R](id: AggregateId[E], command: Command[F, E, A, R]) = {
        partitions(id.hashCode() % partitionNumber)
          .executeCommand(id, command)
      }

      override def fetchMetaAggregate(id: AggregateId[E]) = es.fetchMetaAggregate(id)

      /**
       * Saves the given `meta events`.
       * Should fail in case an event with some id and version is already exist.
       */
      override def saveMetaEvents(events: NonEmptyList[MetaEvent[E]]): F[NonEmptyList[(AggregateId[E], Option[Throwable])]] = {
        events.toList.groupBy(_.aggregateId).map{
          case (id, events) =>
            partitions(id.hashCode() % partitionNumber).saveMetaEvents(NonEmptyList.fromListUnsafe(events))
        }.toList.sequence.map(_.head)
      }
    }
  }
}
