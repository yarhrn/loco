# loco 

This project aims to provide a lightweight, composable library for doing event sourcing. 

The core of loco is very small and built using [tagless final](https://www.youtube.com/watch?v=8sgAUIB8JOY) on top of [cats-effect type classes](https://typelevel.org/cats-effect/typeclasses/).

Right now loco supports PostgreSQL as event storage, but it can be replaced by any other backends by implementing the corresponding [trait](https://github.com/yarhrn/loco/blob/master/core/src/main/scala/loco/repository/EventsRepository.scala).

It is used in production for maintaining premium profile in [Pulsoid](https://pulsoid.net)

## Install
[![](https://jitpack.io/v/yarhrn/loco.svg)](https://jitpack.io/#yarhrn/loco)

```scala
resolvers += "jitpack" at "https://jitpack.io"
//one of
libraryDependencies += "com.github.yarhrn.loco" %% "loco-core" % "v{take version from badge above}"
libraryDependencies += "com.github.yarhrn.loco" %% "loco-doobie" % "v{take version from badge above}"
```

## Core Concepts
TBD