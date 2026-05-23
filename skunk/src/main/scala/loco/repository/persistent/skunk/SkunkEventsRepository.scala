package loco.repository.persistent.skunk

import cats.data.NonEmptyList
import cats.effect.{MonadCancel, Resource}
import cats.implicits._
import _root_.skunk.codec.all._
import _root_.skunk.exception.PostgresErrorException
import _root_.skunk.implicits._
import _root_.skunk.{Codec => SkunkCodec, Command, Query, Session, Void}
import loco.domain.{AggregateId, AggregateVersion, Event, MetaEvent}
import loco.repository.EventsRepository
import loco.repository.EventsRepository.ConcurrentModificationException
import loco.repository.persistent.Codec

import java.time.{Instant, ZoneId}

case class SkunkEventsRepository[F[_], E <: Event](codec: Codec[E],
                                                   session: Resource[F, Session[F]],
                                                   batchSize: Int = 100,
                                                   tableConfiguration: EventsTableConfiguration)
                                                  (implicit MC: MonadCancel[F, Throwable])
  extends EventsRepository[F, E] {

  import tableConfiguration._

  private val eventCodec: SkunkCodec[E] = bytea.imap(codec.decode)(codec.encode)
  private val aggregateIdCodec: SkunkCodec[AggregateId[E]] =
    varchar(36).imap(AggregateId[E](_))(_.id)
  private val aggregateVersionCodec: SkunkCodec[AggregateVersion[E]] =
    int4.imap(AggregateVersion[E](_))(_.version)
  private val instantCodec: SkunkCodec[Instant] =
    timestamp.imap(_.atZone(ZoneId.systemDefault).toInstant)(_.atZone(ZoneId.systemDefault).toLocalDateTime)

  private val metaEventCodec: SkunkCodec[MetaEvent[E]] =
    (aggregateIdCodec *: eventCodec *: instantCodec *: aggregateVersionCodec).imap {
      case (id, event, createdAt, version) => MetaEvent[E](id, event, createdAt, version)
    } { me => (me.aggregateId, me.event, me.createdAt, me.version) }

  private val selectQuery
    : Query[AggregateId[E] *: AggregateVersion[E] *: AggregateVersion[E] *: EmptyTuple, MetaEvent[E]] =
    sql"""select #${aggregateIdColumn}, #${eventColumn}, #${createdAtColumn}, #${aggregateVersionColumn}
          from #${eventsTable}
          where #${aggregateIdColumn} = $aggregateIdCodec
          and #${aggregateVersionColumn} >= $aggregateVersionCodec
          and #${aggregateVersionColumn} <= $aggregateVersionCodec
          order by #${aggregateVersionColumn}""".query(metaEventCodec)

  private val insertCommand: Command[MetaEvent[E]] =
    sql"""insert into #${eventsTable} (#${aggregateIdColumn}, #${eventColumn}, #${createdAtColumn}, #${aggregateVersionColumn})
          values ($metaEventCodec)""".command

  override def fetchEvents(id: AggregateId[E], version: AggregateVersion[E]) = {
    fs2.Stream
      .resource(session)
      .evalMap(_.prepare(selectQuery))
      .flatMap(_.stream(id *: AggregateVersion[E](1) *: version *: EmptyTuple, batchSize))
  }

  override def saveEvents(events: NonEmptyList[MetaEvent[E]]) = {
    session
      .use { s =>
        s.transaction.use { _ =>
          s.prepare(insertCommand).flatMap { ps =>
            events.toList.traverse_(ps.execute)
          }
        }
      }
      .adaptError {
        case e: PostgresErrorException if e.code == "23505" =>
          new ConcurrentModificationException(events.head.aggregateId, events.map(_.version).toList)
      }
  }

}
