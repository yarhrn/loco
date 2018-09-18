package loco

import java.util.{Currency, UUID}

import cats.data.NonEmptyList
import cats.effect.IO
import loco.TransactionStatus.TransactionStatus
import loco.domain.{Aggregate, AggregateId, Event, MetaEvent}
import loco.repository.InMemoryRepository
import loco.view.View

import scala.concurrent.ExecutionContext.Implicits.global

sealed trait TransactionEvent extends Event

case class TransactionCreated(amount: BigDecimal, currency: Currency, providerAccountId: String) extends TransactionEvent

case class TransactionProcessed(providerTransactionId: String) extends TransactionEvent

case class TransactionFailed(errorReason: String) extends TransactionEvent

case class TransactionRefunded() extends TransactionEvent

object TransactionStatus extends Enumeration {
  type TransactionStatus = Value
  val New, Processed, Refunded, Failed = Value
}

case class Transaction(id: AggregateId[TransactionEvent],
                       status: TransactionStatus,
                       amount: BigDecimal,
                       currency: Currency,
                       providerAccountId: String,
                       errorReason: Option[String],
                       providerTransactionId: Option[String]) extends Aggregate[TransactionEvent]

object TransactionBuilder extends AggregateBuilder[Transaction, TransactionEvent] {
  override def empty(id: AggregateId[TransactionEvent]): Transaction = Transaction(id, null, null, null, null, None, None)

  override def apply(aggregate: Transaction, metaEvent: MetaEvent[TransactionEvent]): Transaction = {
    metaEvent.event match {
      case TransactionCreated(amount, currency, providerAccountId) => aggregate.copy(amount = amount, currency = currency, providerAccountId = providerAccountId, status = TransactionStatus.New)
      case TransactionProcessed(providerTransactionId) => aggregate.copy(providerTransactionId = Some(providerTransactionId), status = TransactionStatus.Processed)
      case TransactionFailed(errorReason) => aggregate.copy(errorReason = Some(errorReason), status = TransactionStatus.Failed)
      case TransactionRefunded() => aggregate.copy(status = TransactionStatus.Refunded)
    }
  }
}

object example {

  def main(args: Array[String]): Unit = {
    val repository = InMemoryRepository.unsafeCreate[IO, TransactionEvent]
    val eventSourcing = DefaultEventSourcing[IO, TransactionEvent, Transaction](
      TransactionBuilder,
      repository, // maintains in memory storage of events backed by mutable reference to map
      View.empty, // no views
      ErrorReporter.consoleErrorReporter // reports all error to console
    )

    val transactionId = eventSourcing.saveEvents(NonEmptyList.of(TransactionCreated(5.5, Currency.getInstance("USD"), "profile-id"))).unsafeRunSync()
    println(transactionId)

    val tx = eventSourcing.fetchMetaAggregate(transactionId).unsafeRunSync()
    println(tx)

    eventSourcing.saveEvents(NonEmptyList.of(TransactionProcessed("transaction-id")), transactionId, tx.get.version).unsafeRunSync()

    val tx1 = eventSourcing.fetchMetaAggregate(transactionId).unsafeRunSync()
    println(tx1)
    val metaEvents = repository.fetchEvents(transactionId).compile.toList.unsafeRunSync()
    println(metaEvents.mkString("\n"))
  }

}
