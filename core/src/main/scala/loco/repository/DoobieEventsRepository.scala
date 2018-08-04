package loco.repository

import java.sql.Timestamp
import java.time.Instant

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.meta.Meta
import doobie.util.query.Query
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}

import scala.reflect.runtime.universe.TypeTag

case class DoobieEventsRepository[F[_] : Monad, E <: Event : TypeTag](codec: Codec[E], transactor: Transactor[F], eventsTable: String)
  extends EventsRepository[F, E] {
  implicit val EMeta: Meta[E] = Meta[String].xmap(codec.decode, codec.encode)
  implicit val AggregateVersionMeta: Meta[AggregateVersion[E]] = Meta[Int].xmap(AggregateVersion(_), _.version)
  implicit val AggregateIdMeta: Meta[AggregateId[E]] = Meta[String].xmap(AggregateId(_), _.id)

  import shapeless._

  val selectEvents = s"select * from $eventsTable where aggregate_id = ? and aggregate_version <= ? order by aggregate_version"
  val insertEvents = s"insert into $eventsTable values (?,?,?,?)"

  override def fetchEvents(id: AggregateId[E], version: AggregateVersion[E]) = {
    Query[AggregateId[E] :: AggregateVersion[E] :: HNil, MetaEvent[E]](selectEvents, logHandler0 = LogHandler(println))
      .toQuery0(id :: version :: HNil)
      .stream
      .transact(transactor)
  }

  override def saveEvents(events: NonEmptyList[MetaEvent[E]]) = {
    Update[MetaEvent[E]](insertEvents)
      .updateMany(events)
      .transact(transactor)
      .map(_ => ())
  }
}

object DoobieEventsRepository {
  implicit val InstantMeta: Meta[Instant] = Meta[Timestamp].xmap(_.toInstant, Timestamp.from)
}

trait Codec[E] {
  def encode(e: E): String

  def decode(e: String): E
}