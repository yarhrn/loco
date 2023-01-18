package loco.test

import cats.Applicative

import java.time.Instant
import cats.effect.{Clock, Sync}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{FiniteDuration, TimeUnit}

case class FakeTimer[F[_]: Sync](var millis: Long = System.currentTimeMillis()) {

  def clock = new Clock[F] {
    override def realTime = Sync[F].pure(FiniteDuration(millis, TimeUnit.MILLISECONDS))

    override def monotonic = Sync[F].pure(FiniteDuration(millis, TimeUnit.MILLISECONDS))

    override def applicative: Applicative[F] = Applicative[F]
  }

//  override def sleep(duration: FiniteDuration): F[Unit] = Sync[F].delay(Thread.sleep(duration.toMillis))

  def tick() = {
    millis = millis + 1000
    this
  }

  def instant = Instant.ofEpochMilli(millis)
}
