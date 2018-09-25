# loco 
[![Build Status](https://travis-ci.org/yarhrn/loco.svg?branch=master)](https://travis-ci.org/yarhrn/loco)

This project aims to provide a lightweight, composable library for doing event sourcing. 

The core of loco is very small and built using [tagless final](https://www.youtube.com/watch?v=8sgAUIB8JOY) on top of [cats-effect type classes](https://typelevel.org/cats-effect/typeclasses/).

Right now loco supports only MySQL as event storage, but it can be replaced by any other backends by implementing the corresponding [trait](https://github.com/yarhrn/loco/blob/master/core/src/main/scala/loco/repository/EventsRepository.scala).



## Getting started

- Add loco as a dependency 
```scala
libraryDependencies += "com.yarhrn" %% "loco-core" % "0.0.1"
```
- Define your events
```scala
sealed trait TransactionEvent extends Event

case class TransactionCreated(amount: BigDecimal, currency: Currency, providerAccountId: String) extends TransactionEvent
case class TransactionProcessed(providerTransactionId: String) extends TransactionEvent
case class TransactionFailed(errorReason: String) extends TransactionEvent
case class TransactionRefunded() extends TransactionEvent

```
- Define shape of your aggregate
```scala
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

```
- Define the way how to build aggregate from events
```scala
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
```
- Initialize event sourcing 
```scala
 val repository = InMemoryRepository.unsafeCreate[IO, TransactionEvent]
val eventSourcing = DefaultEventSourcing[IO, TransactionEvent, Transaction](
  TransactionBuilder,
  repository, // maintains in memory storage of events backed by mutable reference to map
  View.empty, // no views
  ErrorReporter.consoleErrorReporter // reports all error to console
)
```
- Emit transaction created event
```scala
val transactionId = eventSourcing.saveEvents(NonEmptyList.of(TransactionCreated(5.5, Currency.getInstance("USD"), "profile-id"))).unsafeRunSync()
AggregateId(f847eb3d-6d87-4cce-9977-8316603e53fc)
```
- Fetch aggregate
```scala
val transcation = eventSourcing.fetchMetaAggregate(transactionId).unsafeRunSync()
Some(MetaAggregate(Transaction(AggregateId(f847eb3d-6d87-4cce-9977-8316603e53fc),New,5.5,USD,profile-id,None,None),AggregateVersion(1)))
```
- Emit transaction processed event. NOTE: we are using version from the previously retrieved aggregate
```scala
eventSourcing.saveEvents(NonEmptyList.of(TransactionProcessed("transaction-id")), transactionId, tx.get.version).unsafeRunSync()
```
- Fetch aggregate, now the transaction is in the processed state
```scala
eventSourcing.fetchMetaAggregate(transactionId).unsafeRunSync()
Some(MetaAggregate(Transaction(AggregateId(f847eb3d-6d87-4cce-9977-8316603e53fc),Processed,5.5,USD,profile-id,None,Some(transaction-id)),AggregateVersion(2)))
```