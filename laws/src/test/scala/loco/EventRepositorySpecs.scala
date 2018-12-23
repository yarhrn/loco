package loco

import cats.Eq
import cats.effect.IO
import loco.domain.{Event, MetaEvent}
import loco.repository.InMemoryRepository
import org.scalatest.FunSuiteLike
import org.typelevel.discipline.scalatest.Discipline
import cats.implicits._
class EventRepositorySpecs extends ArbitraryInstances with Discipline with FunSuiteLike {

  import IncrementEvent._

  implicit def metaEventEq[E <: Event : Eq] = Eq.fromUniversalEquals[MetaEvent[E]]
  implicit def IO[A: Eq]: Eq[IO[A]] = (fx: IO[A], fy: IO[A]) => {
    implicitly[Eq[A]].eqv(fx.unsafeRunSync(), fy.unsafeRunSync())
  }



  checkAll("EventRepository", EventRepositoryAlgebraTests(InMemoryRepository.unsafeCreate[IO, IncrementEvent.type]).algebra)


}