# 3. Unify view interface

Date: 2018-07-23

## Status

Accepted

## Context

There are several interface for view handling :
```scala
trait View[F[_], E <: Event] {
  def handle(event: MetaEvent[E]): F[Unit]
}

trait ViewWithEvents[F[_], E <: Event] {
  def handle(event: MetaEvent[E], events: Iterant[F, MetaEvent[E]]): F[Unit]
}

trait ViewWithAggregate[F[_], A <: Aggregate[E], E <: Event] {
  def handle(event: MetaEvent[E], aggregate: MetaAggregate[E, A]): F[Unit]
}
```
Each of them is introduced to optimize the specific way of view handling.

For example, loco framework should read aggregate only once while handling an arbitrary number of `ViewWithAggregate`.

## Decision

We will substitute these interfaces using one.
```scala
trait View[F[_], E <: Event] {
  def handle(event: NonEmptyList[MetaEvent[E]]): F[Unit]
}
```
As a result, view handling becomes more unified. In terms of new `View` we can implement `ViewWithEvents` and `ViewWithAggregate` etc.
With such generic interface, we decouple specific views from the framework itself.
