package loco.test

import java.time.Instant

import cats.effect.{Clock, Sync, Timer}

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

case class FakeTimer[F[_] : Sync](var millis: Long = System.currentTimeMillis()) extends Timer[F] {

  override def clock = new Clock[F] {
    override def realTime(unit: TimeUnit) = Sync[F].pure(millis)

    override def monotonic(unit: TimeUnit) = Sync[F].pure(millis)
  }

  override def sleep(duration: FiniteDuration): F[Unit] = Sync[F].delay(Thread.sleep(duration.toMillis))

  def tick() = {
    millis = millis + 1000
    this
  }

  def instant = Instant.ofEpochMilli(millis)
}
