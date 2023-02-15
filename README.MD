# loco 

This project aims to provide a lightweight, composable library for building event sourced applications. 

The core of `loco` is compact and built using [tagless final](https://www.youtube.com/watch?v=8sgAUIB8JOY) on top of [cats-effect type classes](https://typelevel.org/cats-effect/docs/typeclasses/).

Right now loco supports PostgreSQL as event storage, but it can be replaced by any other backends by implementing the corresponding [trait](https://github.com/yarhrn/loco/blob/master/core/src/main/scala/loco/repository/EventsRepository.scala).

It is used in production for maintaining premium profiles in [Pulsoid](https://pulsoid.net)

## Install
[![](https://jitpack.io/v/yarhrn/loco.svg)](https://jitpack.io/#yarhrn/loco)

```scala
resolvers += "jitpack" at "https://jitpack.io"
//one of
libraryDependencies += "com.github.yarhrn.loco" %% "loco-core" % "{take version from badge above}"
libraryDependencies += "com.github.yarhrn.loco" %% "loco-doobie" % "{take version from badge above}"
```

## Glossary

Event - persisted action that occurred, together with any associated data that's required to describe the action represented by the event.

AggregateId - identifier of the entity, e.g. payment id, order id.

Aggregate - folded representation of the events for the entity with the given AggregateId.

Command - describes how to produce events for the given aggregate and additional data. For example "item added to basket" command accept the item identifier and will produce "item added" event if the basket has not been paid already.  

View - describes an action that should be performed as a reaction to the event. For example, on the event "basket paid" send a confirmation email.

EventRepository - storage of the events. Provide the ability to fetch events by AggregateId and atomically store events via optimistic locking.

## Building Event Sourced Application with loco
To build an event-sourced application first of all you should think about aggregates in your domain.
>A DDD aggregate is a cluster of domain objects that can be treated as a single unit. An example may be an order and its line-items, these will be separate objects, but it's useful to treat the order (together with its line items) as a single aggregate. (source https://www.martinfowler.com/bliki/DDD_Aggregate.html)

In `loco` changes to aggregate are described via events. Let's define events for the Order aggregate.
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
Each event is associated with concrete Order by AggregateId.

Order Aggregate can be described as follows 
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

Order Aggregate is built by folding a list of events.

Folding logic can be described as a function that accepts the previous Order Aggregate and Event and returns the next Order Aggregate.
Creating a new Event for the Order Aggregate is done via Command. A command is a consistency boundary. Command to add the item to the Order Aggregate can be described as follows
```
class AddItemCommand(id, sku, quantity)
    def events(orderAggregate):
        if orderAggregate is not paid
            return List(ItemAdded(id, sku, quantity))
        else
            return List() // or fail with an error
```

A command should check domain rules and produce events in case everything is ok. A command is executed using an optimistic lock. In case parallel process has inserted new events for the particular Order Aggregate execution of the AddItemCommand will fail. 

`loco`'s implementation of Event Sourcing is simple and powerful. At the same time, it has its own limitations. 

_Limitation 1._ **Aggregates should be bounded by the number of events.** As for each command execution it is required to rebuild a current state of aggregate by loading all aggregate's events it is not possible to describe aggregates with an infinite number of events. For example, a financial account with a potentially infinite number of debit and credit records is not a good aggregate as loading all events can potentially take a lot of time. 

_Limitation 2._ **Aggregates should be semantically single-threaded.** Aggregates that often change in parallel will result in a lot of concurrent modifications error which will decrease the overall performance of the application and indicates that the choice of aggregate is bad for event sourcing in general or `loco`'s implementation of event sourcing in particular. For example, an aggregate that describes visits to a particular web page can be changed in parallel by big amount of users and is not a good use case for `loco`.

