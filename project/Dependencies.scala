import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "4.1.0" % Test

  lazy val mysql = "mysql" % "mysql-connector-java" % "8.0.12" % Test
  lazy val embeddedMysql = "com.wix" % "wix-embedded-mysql" % "4.1.2" % Test

  lazy val doobieCore = "org.tpolecat" %% "doobie-core" % "0.6.0-M2"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "1.0.0"
  lazy val fs2Core = "co.fs2" %% "fs2-core" % "1.0.0-M5"

  lazy val jsoniter = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "0.29.20"
  lazy val jsoniterMacros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "0.29.20"

  lazy val mongodbClient = "org.mongodb" % "mongodb-driver-async" % "3.8.2"

  lazy val mongodbEmbedded = "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.1.1" % Test
}