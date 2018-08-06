package loco.test

import java.time.Instant

import cats.Monad
import cats.effect.{Sync, Timer}

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

case class FakeTimer[F[_] : Sync](var millis: Long = System.currentTimeMillis()) extends Timer[F] {
  override def clockRealTime(unit: TimeUnit): F[Long] = clockMonotonic(unit)

  override def clockMonotonic(unit: TimeUnit): F[Long] = Sync[F].pure(millis)

  override def sleep(duration: FiniteDuration): F[Unit] = Sync[F].delay(Thread.sleep(duration.toMillis))

  override def shift: F[Unit] = Monad[F].unit

  def tick() = {
    millis = millis + 1000
    this
  }

  def instant = Instant.ofEpochMilli(millis)
}
