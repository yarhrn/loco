package loco

import cats.data.NonEmptyList
import cats.effect.Sync
import loco.domain.{AggregateId, Event, MetaEvent}
import loco.repository.EventsRepository
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws
import org.typelevel.discipline.Laws
import cats.kernel.laws.discipline._
import cats.Eq
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._

trait EventRepositoryTests[F[_], E <: Event] extends Laws {

  def laws: EventRepoLaws[F, E]


  def algebra(implicit arbEmail: Arbitrary[NonEmptyList[MetaEvent[E]]],
              arbId: Arbitrary[AggregateId[E]],
              eqFOptEmail: Eq[F[List[MetaEvent[E]]]]) =
    new SimpleRuleSet(
      name = "EventRepository",
      "fetchRetrievedEvents" -> forAll(laws.fetchRetrievedEvents _),
      "fetchRandomIdReturnNoEvents" -> forAll(laws.fetchRandomIdReturnNoEvents _),
      "saveByOneSameAsInBatch" -> forAll(laws.saveByOneSameAsInBatch _)
    )
}

object EventRepositoryAlgebraTests {

  def apply[F[_] : Sync, E <: Event](instance: EventsRepository[F, E]) = new EventRepositoryTests[F, E] {
    override def laws = EventRepoLaws(instance)
  }
}

