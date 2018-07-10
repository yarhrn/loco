package loco

import cats.data.NonEmptyList
import cats.effect.Sync
import doobie.util.transactor.Transactor
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import monix.execution.misc.NonFatal
import monix.tail.Iterant
import monix.tail.Iterant.{Halt, Last, Next, Suspend}

import scala.language.higherKinds

package object repository {

  trait EventsRepository[F[_], E <: Event] {

    def fetchEvents(id: AggregateId[E], version: Option[AggregateVersion[E]] = None): Iterant[F, MetaEvent[E]]

    def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit]

  }

  class DoobieEventRepository[F[_], E <: Event](transactor: Transactor[F]) extends EventsRepository[F, E] {

    override def fetchEvents(id: AggregateId[E], version: Option[AggregateVersion[E]]): Iterant[F, MetaEvent[E]] = ???

    override def saveEvents(events: NonEmptyList[MetaEvent[E]]): F[Unit] = ???

  }

  object DoobieEventRepository {

    case class Page(page: Int, count: Int)

    def fromStateActionTerminatedL[F[_], S, A](f: S => F[(A, Option[S])])(seed: => F[S])(implicit F: Sync[F]): Iterant[F, A] = {
      import cats.syntax.all._

      def loop(state: S): F[Iterant[F, A]] =
        try {
          f(state).map {
            case (elem, Some(newState)) =>
              Next(elem, F.suspend(loop(newState)), F.unit)
            case (elem, None) =>
              Last(elem)
          }
        } catch {
          case e if NonFatal(e) => F.pure(Halt(Some(e)))
        }

      Suspend(F.suspend(seed.flatMap(loop)), F.unit)
    }

  }

}
