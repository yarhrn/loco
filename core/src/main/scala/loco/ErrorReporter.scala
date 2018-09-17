package loco

import cats.MonadError
import cats.effect.Sync
import scala.language.higherKinds

/**
  * Reports exceptions occurred, while asynchronous actions execution(view handling, etc.)
  */
trait ErrorReporter[F[_]] {
  /**
    * Reports given `throwable`.
    * Should never end with error in scope of F.
    */
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

  def consoleErrorReporter[F[_]](implicit S: Sync[F]): ErrorReporter[F] = (e: Throwable) => S.delay(e.printStackTrace())

}