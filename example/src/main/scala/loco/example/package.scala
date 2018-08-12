package loco


import java.util.Currency

import _root_.loco.domain.{Aggregate, AggregateId, Event, MetaEvent}
import loco.example.TransactionStatus.TransactionStatus

package object example {


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


}
