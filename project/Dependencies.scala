import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.20" % Test
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "7.5.5" % Test

  lazy val postgresql = "org.postgresql" % "postgresql" % "42.7.12" % Test
  lazy val embeddedPostgresql = "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.44.1" % "test"

  lazy val doobieCore = "org.tpolecat" %% "doobie-core" % "1.0.0-RC12"

  lazy val skunkCore = "org.tpolecat" %% "skunk-core" % "1.0.0-M10"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.7.0"
  lazy val catsEffectStd = "org.typelevel" %% "cats-effect-std" % "3.7.0"
  lazy val fs2Core = "co.fs2" %% "fs2-core" % "3.13.0"

  lazy val jsoniter = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.38.12"
  lazy val jsoniterMacros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.38.12"

}
