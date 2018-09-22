package loco.repository.persistent.sql

import java.nio.charset.StandardCharsets

import cats.Invariant
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, _}

trait Codec[E] {
  def encode(e: E): String

  def decode(e: String): E
}


object Codec {

  implicit val CodecInvariant = new Invariant[Codec] {
    override def imap[A, B](fa: Codec[A])(f: A => B)(g: B => A) = new Codec[B] {
      override def encode(e: B) = fa.encode(g(e))

      override def decode(e: String) = f(fa.decode(e))
    }
  }

  def apply[E: Codec] = implicitly[Codec[E]]

  def fromJsonCodec[A](jsonValueCodec: JsonValueCodec[A]) = {
    new Codec[A] {
      override def encode(e: A) = {
        new String(writeToArray(e)(jsonValueCodec), StandardCharsets.UTF_8)
      }

      override def decode(e: String) = {
        readFromArray(e.getBytes(StandardCharsets.UTF_8))(jsonValueCodec)
      }
    }
  }

}