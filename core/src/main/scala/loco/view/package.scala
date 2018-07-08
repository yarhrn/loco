package loco

import loco.domain._
import monix.tail.Iterant

import scala.language.higherKinds

package object view {

  trait View[F[_], E] {
    def handle(event: MetaEvent[E]): F[Unit]
  }

  trait ViewWithEvents[F[_], E] {
    def handle(event: MetaEvent[E], events: Iterant[F, MetaEvent[E]]): F[Unit]
  }

  trait ViewWithAggregate[F[_], A, E] {
    def handle(event: MetaEvent[E], aggregate: A): F[Unit]
  }

}
