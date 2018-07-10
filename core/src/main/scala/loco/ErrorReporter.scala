package loco

trait ErrorReporter[F[_]] {
  def error(throwable: Throwable): F[Unit]
}
