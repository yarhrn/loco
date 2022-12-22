package loco

import java.time.Instant

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.kernel.Eq
import cats.kernel.laws._
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import loco.repository.EventsRepository
import org.scalacheck.{Arbitrary, Gen}
import cats.implicits._

trait EventRepoLaws[F[_], E <: Event] {
  def repo: EventsRepository[F, E]
  implicit val F: Sync[F]

  def fetchRetrievedEvents(e: NonEmptyList[MetaEvent[E]]) = {
    repo.saveEvents(e) >> repo.fetchEvents(e.head.aggregateId).compile.toList <-> F.pure(e.toList)
  }

  def saveByOneSameAsInBatch(e: NonEmptyList[MetaEvent[E]]) = {
    e.map(event => repo.saveEvents(NonEmptyList.of(event))).sequence >> repo.fetchEvents(e.head.aggregateId).compile.toList <-> F.pure(e.toList)
  }

  def fetchRandomIdReturnNoEvents(id: AggregateId[E]) = {
    repo.fetchEvents(id).compile.toList <-> F.pure(List())
  }

}

case object IncrementEvent extends Event {
  implicit val i = Eq.fromUniversalEquals[IncrementEvent.type]
}


trait ArbitraryInstances {

  implicit val incrementEventGen = Gen.const(IncrementEvent)

  implicit def aggregateIdGen[E <: Event] = for {
    uuid <- Gen.uuid
  } yield AggregateId[E](uuid.toString)


  implicit def metaEventsGen[E <: Event](implicit IdGen: Gen[AggregateId[E]],
                                         eGen: Gen[E]) = for {
    id <- IdGen
    maxVersion <- Gen.chooseNum(2, 100)
    events <- Gen.listOfN(maxVersion, eGen)
  } yield {
    val metaEvents = events.zipWithIndex.map(v => MetaEvent(id, v._1, Instant.now(), AggregateVersion(v._2)))
    NonEmptyList.fromListUnsafe(metaEvents)
  }

  implicit val incrementEvent: Arbitrary[IncrementEvent.type] = Arbitrary(Gen.const(IncrementEvent))
  implicit def metaEventArb[E <: Event](implicit gen: Gen[NonEmptyList[MetaEvent[E]]]) = Arbitrary(gen)
  implicit def idArb[E <: Event] = Arbitrary(aggregateIdGen[E])
}


object EventRepoLaws {

  def apply[F[_], E <: Event](instance: EventsRepository[F, E])(implicit M: Sync[F]) = new EventRepoLaws[F, E] {
    override def repo = instance
    override implicit val F: Sync[F] = M
  }

}