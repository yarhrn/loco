package loco

import cats.MonadError

import scala.language.higherKinds

trait ErrorReporter[F[_]] {
  def error(throwable: Throwable): F[Unit]
}


object ErrorReporter {

  implicit class ErrorReporterOps[F[_]](fa: F[Unit])(implicit ME: MonadError[F, Throwable], ER: ErrorReporter[F]) {

    import scala.util.control.NonFatal
    import cats.implicits._

    def reportError: F[Unit] = fa.recoverWith {
      case NonFatal(ex) => ER.error(ex)
    }
  }

}