package loco.test

import cats.effect.Sync
import loco.ErrorReporter
import org.scalatest._


case class ConsoleErrorReporter[F[_] : Sync]() extends ErrorReporter[F] {
  var errors: List[Throwable] = List()

  override def error(throwable: Throwable): F[Unit] = {
    Sync[F].delay {
      errors = errors :+ throwable
      println("error occured")
      throwable.printStackTrace()
    }
  }
}

trait ConsoleErrorReporterMatcher[F[_]] {

  import matchers._

  def haveError: Matcher[ConsoleErrorReporter[F]] = {
    (left: ConsoleErrorReporter[F]) =>
      MatchResult(left.errors.nonEmpty, s"no errors occurred, but expected", s"error occurred ${left.errors}, but no expected", IndexedSeq())
  }

  def haveExactError(ex: Throwable): Matcher[ConsoleErrorReporter[F]] = {
    (left: ConsoleErrorReporter[F]) =>
      MatchResult(left.errors.head == ex && left.errors.size == 1, s"errors occurred ${left.errors}, but expected $ex", s"error occurred ${left.errors}, but expected ${ex}", IndexedSeq())
  }

}