package loco.view

import cats.data.NonEmptyList
import cats.effect.{IO, Ref}
import cats.effect.kernel.Sync
import loco.domain.{Event, MetaEvent}
import cats.implicits._
import loco.ErrorReporter

class DynamicViews[F[_]: Sync, E <: Event](views: Ref[F, List[View[F, E]]])(implicit er: ErrorReporter[F])
    extends View[F, E] {

  override def handle(events: NonEmptyList[MetaEvent[E]]): F[Unit] = {
    views.get.flatMap { views => View.wrap(views).handle(events) }
  }

  def add(view: View[F, E]): F[Unit] = {
    views.update(_ :+ view)
  }

}
