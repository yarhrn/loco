import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19" % Test
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "6.0.0" % Test

  lazy val postgresql = "org.postgresql" % "postgresql" % "42.7.3" % Test
  lazy val embeddedPostgresql = "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.41.4" % "test"

  lazy val doobieCore = "org.tpolecat" %% "doobie-core" % "1.0.0-RC9"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.6.0"
  lazy val catsEffectStd = "org.typelevel" %% "cats-effect-std" % "3.6.0"
  lazy val fs2Core = "co.fs2" %% "fs2-core" % "3.12.0"

  lazy val jsoniter = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.30.7"
  lazy val jsoniterMacros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.30.7"

}
