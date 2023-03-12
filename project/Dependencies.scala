import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15" % Test
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "4.4.0" % Test

  lazy val postgresql = "org.postgresql" % "postgresql" % "42.5.4" % Test
  lazy val embeddedPostgresql = "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.12" % "test"

  lazy val doobieCore = "org.tpolecat" %% "doobie-core" % "1.0.0-RC2"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.14"
  lazy val catsEffectStd = "org.typelevel" %% "cats-effect-std" % "3.4.8"
  lazy val fs2Core = "co.fs2" %% "fs2-core" % "3.2.14"
  lazy val fs2Reactive = "co.fs2" %% "fs2-reactive-streams" % "2.5.6"

  lazy val jsoniter = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.21.3"
  lazy val jsoniterMacros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.21.3"

  lazy val mongodbReactiveStreams = "org.mongodb" % "mongodb-driver-reactivestreams" % "1.13.0"

  lazy val mongodbEmbedded = "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.2.0" % Test
}
