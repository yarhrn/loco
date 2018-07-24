package loco.view

import cats.data.NonEmptyList
import loco.domain._

import scala.language.higherKinds


trait View[F[_], E <: Event] {
  def handle(event: NonEmptyList[MetaEvent[E]]): F[Unit]
}


