package loco

import cats.Functor
import cats.implicits._

package object util {

  implicit class Unitify[F[_] : Functor, A](fa: F[A]) {
    def unitify: F[Unit] = fa.map(_ => ())
  }

}
