import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "4.1.0" % Test

  lazy val cats = "org.typelevel" %% "cats-core" % "1.0.1"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "1.0.0-RC2"
  lazy val fs2 = "co.fs2" %% "fs2-core" % "0.10.4"
  lazy val doobie = "org.tpolecat" %% "doobie-core" % "0.5.3"
}
