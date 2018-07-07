ad# 2. Core concept is Event

Date: 2018-07-08

## Status

Proposed

## Context

As we want to build the type-safe framework, we should somehow mark that 

- aggregate version
- aggregate id 
- etc

belong to same aggregate or even entity in our system.
As we are talking about event sourcing, the central concept here is an event.
Aggregate itself is some way concrete way of events folding.
As a result, I don't want to couple those things to aggregate as an aggregate is a matter of change.


## Decision

I propose to add type parameter to 
- aggregate version
- aggregate id 

e.g.
```scala
case class AggregateId[E](id: String)
case class AggregateVersion[E](version: Int)
```
and type parameter `E` should be an `Event` type rather that `Aggregate` type

## Consequences

Consequences here...
