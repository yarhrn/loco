import Dependencies._

lazy val core = (project in file("core")).
  settings(
    inThisBuild(List(
      organization := "loco",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "core",
    libraryDependencies ++= Seq(
      cats,
      catsEffect,
      monix,
      scalaTest % Test
    )
  )
