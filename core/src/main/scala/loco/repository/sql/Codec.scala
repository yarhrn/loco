package loco.repository.sql

import java.nio.charset.StandardCharsets

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, _}

trait Codec[E] {
  def encode(e: E): String

  def decode(e: String): E
}


object Codec {
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