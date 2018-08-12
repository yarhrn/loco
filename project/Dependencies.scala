import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "4.1.0" % Test

  lazy val mysql = "mysql" % "mysql-connector-java" % "8.0.12" % Test
  lazy val embeddedMysql = "com.wix" % "wix-embedded-mysql" % "4.1.2" % Test

  lazy val doobie = "org.tpolecat" %% "doobie-core" % "0.6.0-M2"
}
