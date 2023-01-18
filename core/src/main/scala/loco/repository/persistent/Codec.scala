package loco.repository.persistent

import cats.Invariant
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, _}

trait Codec[E] {
  def encode(e: E): Array[Byte]

  def decode(e: Array[Byte]): E
}

object Codec {

  implicit val CodecInvariant = new Invariant[Codec] {
    override def imap[A, B](fa: Codec[A])(f: A => B)(g: B => A) = new Codec[B] {
      override def encode(e: B) = fa.encode(g(e))

      override def decode(e: Array[Byte]) = f(fa.decode(e))
    }
  }

  def apply[E: Codec] = implicitly[Codec[E]]

  def fromJsonCodec[A](jsonValueCodec: JsonValueCodec[A]) = {
    new Codec[A] {
      override def encode(e: A) = {
        writeToArray(e)(jsonValueCodec)
      }

      override def decode(e: Array[Byte]) = {
        readFromArray(e)(jsonValueCodec)
      }
    }
  }

}
