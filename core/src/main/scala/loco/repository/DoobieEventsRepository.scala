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

case class DoobieEventsRepository[F[_] : Monad, E <: Event : TypeTag](codec: Codec[E],
                                                                      transactor: Transactor[F],
                                                                      eventsTable: String,
                                                                      logHandler: LogHandler = LogHandler.nop,
                                                                      batchSize: Int = 100)
  extends EventsRepository[F, E] {
  implicit val EMeta: Meta[E] = Meta[String].xmap(codec.decode, codec.encode)
  implicit val AggregateVersionMeta: Meta[AggregateVersion[E]] = Meta[Int].xmap(AggregateVersion(_), _.version)
  implicit val AggregateIdMeta: Meta[AggregateId[E]] = Meta[String].xmap(AggregateId(_), _.id)

  import shapeless._

  val selectEvents = s"select * from $eventsTable where aggregate_id = ? and aggregate_version >= ? and aggregate_version <= ? order by aggregate_version"
  val insertEvents = s"insert into $eventsTable values (?,?,?,?)"

  override def fetchEvents(id: AggregateId[E], version: AggregateVersion[E]) = {
    val rawId = id.id

    fs2.Stream.unfoldEval[F, StreamState, List[MetaEvent[E]]](StreamState.start(version.version)) {
      case Stop => Monad[F].pure(Option.empty)
      case state: Continue => fetch(rawId, state.from, state.to).map {
        events =>
          if (events.size != batchSize + 1) {
            Some((events, Stop))
          } else {
            Some((events, state.next))
          }
      }
    }.flatMap(events => fs2.Stream(events: _*))
  }

  private def fetch(id: String, from: Int, to: Int) = {
    Query[String :: Int :: Int :: HNil, MetaEvent[E]](selectEvents, logHandler0 = logHandler)
      .toQuery0(id :: from :: to :: HNil)
      .to[List]
      .transact(transactor)
  }

  override def saveEvents(events: NonEmptyList[MetaEvent[E]]) = {
    Update[MetaEvent[E]](insertEvents, logHandler0 = logHandler)
      .updateMany(events)
      .transact(transactor)
      .map(_ => ())
  }

  sealed trait StreamState

  case class Continue(from: Int, to: Int, maxTo: Int) extends StreamState {
    def next = {
      val nextTo = to + 1
      if (nextTo > maxTo) {
        Stop
      } else {
        Continue(nextTo, (nextTo + batchSize).min(maxTo), maxTo)
      }
    }
  }

  case object Stop extends StreamState

  object StreamState {
    def start(maxVersion: Int): StreamState = Continue(1, (1 + batchSize).min(maxVersion), maxVersion)
  }

}

object DoobieEventsRepository {
  implicit val InstantMeta: Meta[Instant] = Meta[Timestamp].xmap(_.toInstant, Timestamp.from)
}

trait Codec[E] {
  def encode(e: E): String

  def decode(e: String): E
}