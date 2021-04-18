# loco 
[![Build Status](https://travis-ci.org/yarhrn/loco.svg?branch=master)](https://travis-ci.org/yarhrn/loco)

This project aims to provide a lightweight, composable library for doing event sourcing. 

The core of loco is very small and built using [tagless final](https://www.youtube.com/watch?v=8sgAUIB8JOY) on top of [cats-effect type classes](https://typelevel.org/cats-effect/typeclasses/).

Right now loco supports MySQL or MongoDB as event storage, but it can be replaced by any other backends by implementing the corresponding [trait](https://github.com/yarhrn/loco/blob/master/core/src/main/scala/loco/repository/EventsRepository.scala).

It is used in production for maintaining premium profile in [Pulsoid](https://pulsoid.net)

## Getting started

- Add loco as a dependency 
```scala
resolvers += "jitpack" at "https://jitpack.io"
//one of
libraryDependencies += "com.github.yarhrn.loco" % "core_2.12" % "v0.2.13"	
libraryDependencies += "com.github.yarhrn.loco" % "doobie_2.12" % "v0.2.13"	
libraryDependencies += "com.github.yarhrn.loco" % "mongodb_2.12" % "v0.2.13"
```
- Basic usage
```scala
package loco

import java.util.Currency

import cats.data.NonEmptyList
import cats.effect.IO
import loco.TransactionStatus.TransactionStatus
import loco.domain.{Aggregate, AggregateId, Event, MetaEvent}
import loco.repository.InMemoryRepository
import loco.view.View

import scala.concurrent.ExecutionContext

// Events
sealed trait TransactionEvent extends Event
case class TransactionCreated(amount: BigDecimal, currency: Currency, providerAccountId: String) extends TransactionEvent
case class TransactionProcessed(providerTransactionId: String) extends TransactionEvent
case class TransactionFailed(errorReason: String) extends TransactionEvent
case class TransactionRefunded() extends TransactionEvent

object TransactionStatus extends Enumeration {
  type TransactionStatus = Value
  val New, Processed, Refunded, Failed = Value
}

// Aggregate
case class Transaction(id: AggregateId[TransactionEvent],
                       status: TransactionStatus,
                       amount: BigDecimal,
                       currency: Currency,
                       providerAccountId: String,
                       errorReason: Option[String],
                       providerTransactionId: Option[String]) extends Aggregate[TransactionEvent]

// Aggregate builder
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
    implicit val _ = IO.timer(ExecutionContext.global)
    val repository = InMemoryRepository.unsafeCreate[IO, TransactionEvent]

    val eventSourcing = DefaultEventSourcing[IO, TransactionEvent, Transaction](
      TransactionBuilder,
      repository, // maintains in memory storage of events backed by mutable reference to map
      View.empty, // no views
      ErrorReporter.consoleErrorReporter // reports all error to console
    )

    val transactionId = eventSourcing.saveEvents(NonEmptyList.of(TransactionCreated(5.5, Currency.getInstance("USD"), "profile-id"))).unsafeRunSync()
    println(transactionId) // AggregateId(1dad1044-5812-4558-a687-662fafb5d5fe)

    val tx = eventSourcing.fetchMetaAggregate(transactionId).unsafeRunSync()
    println(tx) // Some(MetaAggregate(Transaction(AggregateId(1dad1044-5812-4558-a687-662fafb5d5fe),New,5.5,USD,profile-id,None,None),AggregateVersion(1)))


    eventSourcing.saveEvents(NonEmptyList.of(TransactionProcessed("transaction-id")), transactionId, tx.get.version).unsafeRunSync()

    val tx1 = eventSourcing.fetchMetaAggregate(transactionId).unsafeRunSync()
    println(tx1) // Some(MetaAggregate(Transaction(AggregateId(1dad1044-5812-4558-a687-662fafb5d5fe),Processed,5.5,USD,profile-id,None,Some(transaction-id)),AggregateVersion(2)))

    val metaEvents = repository.fetchEvents(transactionId).compile.toList.unsafeRunSync()
    println(metaEvents.mkString("\n")) // MetaEvent(AggregateId(1dad1044-5812-4558-a687-662fafb5d5fe),TransactionCreated(5.5,USD,profile-id),2019-04-16T10:45:11.787Z,AggregateVersion(1))
                                       //MetaEvent(AggregateId(1dad1044-5812-4558-a687-662fafb5d5fe),TransactionProcessed(transaction-id),2019-04-16T10:45:12.011Z,AggregateVersion(2))
  }

}

```
