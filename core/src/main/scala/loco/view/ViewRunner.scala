package loco.view

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.Sync
import loco.ErrorReporter
import loco.domain._
import monix.tail.Iterant
import cats.implicits._
import loco.repository.EventsRepository
import loco.util._
import monix.tail.Iterant.Last

import scala.language.higherKinds

/*
TODO: suspend all call to view in F
TODO: optimize aggregate constructing and stream of events constructing  for views
 */
case class ViewRunner[F[_] : Sync, E <: Event, A <: Aggregate[E]](views: List[View[F, E]],
                                                                  viewsWithAggregates: List[ViewWithAggregate[F, A, E]],
                                                                  viewsWithEvents: List[ViewWithEvents[F, E]],
                                                                  repository: EventsRepository[F, E],
                                                                  aggregateBuilder: MetaAggregateBuilder[E, A],
                                                                  errorReporter: ErrorReporter[F]) {

  def notify(metaEvents: NonEmptyList[MetaEvent[E]]): F[Unit] = {
    val metaEventsList = metaEvents.toList
    val id = metaEventsList.head.aggregateId
    for {


      _ <- List(
        notifyViews(metaEventsList),
        notifyViewsWithEvents(id, metaEventsList),
        notifyWithAggregate(id, metaEventsList))
        .sequence
        .unitify
        .recoverWith {
          case ex => errorReporter.error(ex)
        }
    } yield ()
  }

  private def notifyViews(events: List[MetaEvent[E]]): F[Unit] = {
    val actions: List[F[Unit]] = for {
      view <- views
      event <- events
    } yield {
      view.handle(event)
    }
    actions.sequence.unitify.adaptError {
      case ex => new RuntimeException(ex)
    }
  }

  private def notifyViewsWithEvents(id: AggregateId[E], events: List[MetaEvent[E]]): F[Unit] = {
    val actions: List[F[Unit]] = for {
      event <- events

      view <- viewsWithEvents
    } yield {
      val allEvents: Iterant[F, MetaEvent[E]] = repository.fetchEvents(id, Some(event.version))
      view.handle(event, allEvents).recoverWith {
        case ex => errorReporter.error(ex)
      }
    }
    actions.sequence.unitify.adaptError {
      case ex => new RuntimeException(ex)
    }
  }

  private def notifyWithAggregate(id: AggregateId[E], events: List[MetaEvent[E]]): F[Unit] = {

    if (views.nonEmpty) {
      val actions: List[F[Unit]] = for {
        event <- events
      } yield {
        {
          for {
            aggregate <- repository.fetchEvents(id).foldLeftL(aggregateBuilder.empty(id))((a, e) => aggregateBuilder.apply(a, e))
            _ <- viewsWithAggregates.map(view => view.handle(event, aggregate)).sequence
          } yield ()
        }.recoverWith {
          case ex => errorReporter.error(ex)
        }

      }
      actions.sequence.unitify.adaptError {
        case ex => new RuntimeException(ex)
      }
    } else {
      Monad[F].unit
    }
  }
}
