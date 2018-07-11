package loco.test

import cats.effect.Sync
import loco.ErrorReporter
import org.scalatest._


class ConsoleErrorReporter[F[_] : Sync] extends ErrorReporter[F] {
  var errors: List[Throwable] = List()

  override def error(throwable: Throwable): F[Unit] = Sync[F].delay {
    errors = errors :+ throwable
    println("error occured")
    throwable.printStackTrace()
  }
}

trait ConsoleErrorReporterMatcher[F[_]] {

  import matchers._

  def haveError: Matcher[ConsoleErrorReporter[F]] = {
    (left: ConsoleErrorReporter[F]) =>
      MatchResult(left.errors.nonEmpty, s"no errors occurred, but expected", s"error occurred ${left.errors}, but no expected", IndexedSeq())
  }

}