package loco.repository.sql

trait Codec[E] {
  def encode(e: E): String

  def decode(e: String): E
}
