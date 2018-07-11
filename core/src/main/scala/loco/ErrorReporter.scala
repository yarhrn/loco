package loco

import scala.language.higherKinds

trait ErrorReporter[F[_]] {
  def error(throwable: Throwable): F[Unit]
}
