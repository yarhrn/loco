# loco

A lightweight, composable library for building event sourced applications.

The core of `loco` is compact and built using [tagless final](https://www.youtube.com/watch?v=8sgAUIB8JOY) on top of [cats-effect type classes](https://typelevel.org/cats-effect/docs/typeclasses/).

Right now loco supports PostgreSQL as event storage via two implementations: [Doobie](https://tpolecat.github.io/doobie/) (`loco-doobie`) and [Skunk](https://github.com/typelevel/skunk) (`loco-skunk`, experimental). It can be replaced by any other backend by implementing the corresponding [trait](https://github.com/yarhrn/loco/blob/master/core/src/main/scala/loco/repository/EventsRepository.scala).

Requires **Scala 3.3.7** or later.

---

## Install

[![](https://jitpack.io/v/yarhrn/loco.svg)](https://jitpack.io/#yarhrn/loco)

```scala
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.yarhrn.loco" %% "loco-core" % "{take version from badge above}"
// PostgreSQL via Doobie
libraryDependencies += "com.github.yarhrn.loco" %% "loco-doobie" % "{take version from badge above}"
// PostgreSQL via Skunk (experimental)
libraryDependencies += "com.github.yarhrn.loco" %% "loco-skunk" % "{take version from badge above}"
```

---

## Glossary

| Term | Description |
|------|-------------|
| **Event** | Persisted action that occurred, together with any associated data required to describe it. |
| **AggregateId** | Identifier of the entity, e.g. payment id, order id. |
| **Aggregate** | Folded representation of the events for the entity with the given AggregateId. |
| **Command** | Describes how to produce events for a given aggregate. For example, an "add item to basket" command accepts the item identifier and produces an "item added" event if the basket has not been paid already. |
| **View** | An action performed as a reaction to an event. For example, on the event "basket paid" send a confirmation email. |
| **EventRepository** | Storage of events. Provides the ability to fetch events by AggregateId and atomically store events via optimistic locking. |

---

## Building Event Sourced Application with loco

To build an event-sourced application, first think about the aggregates in your domain.

> A DDD aggregate is a cluster of domain objects that can be treated as a single unit. An example may be an order and its line-items — these will be separate objects, but it's useful to treat the order (together with its line items) as a single aggregate.
> — [Martin Fowler](https://www.martinfowler.com/bliki/DDD_Aggregate.html)

In `loco`, changes to an aggregate are described via events. Let's define events for the Order aggregate:

```
OrderCreated
    - owner id

ItemAdded
    - id
    - sku
    - quantity

ItemRemoved
    - id

OrderPaid
    - payment id

OrderFulfilled
    - fulfillment details
```

Each event is associated with a concrete Order by AggregateId.

The Order Aggregate can be described as follows:

```
Item
    - sku
    - quantity

Order
    - order id
    - owner id
    - list of Items
    - optional fulfillment details
    - optional payment id
```

The Order Aggregate is built by **folding** a list of events. The folding logic is a function that accepts the previous Order Aggregate and an Event, and returns the next Order Aggregate.

Creating a new Event for the Order Aggregate is done via a **Command**. A command is a consistency boundary:

```scala
class AddItemCommand(id, sku, quantity)
    def events(orderAggregate):
        if orderAggregate is not paid
            return List(ItemAdded(id, sku, quantity))
        else
            return List() // or fail with an error
```

A command checks domain rules and produces events when everything is valid. Commands are executed using an optimistic lock — if a parallel process has inserted new events for the same Order Aggregate, execution of the `AddItemCommand` will fail.

### Limitations

`loco`'s implementation of Event Sourcing is simple and powerful, but it has its own limitations:

1. **Aggregates should be bounded by the number of events.** Each command execution rebuilds the current state by loading all of the aggregate's events, so aggregates with an unbounded number of events are not practical. For example, a financial account with a potentially infinite number of debit and credit records is not a good aggregate candidate.

2. **Aggregates should be semantically single-threaded.** Aggregates that frequently change in parallel will produce many concurrent modification errors, decreasing overall performance. For example, an aggregate tracking visits to a web page — changed concurrently by many users — is not a good use case for `loco`.
