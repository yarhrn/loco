import Dependencies._

val common = List(
  organization := "loco",
  scalaVersion := "2.12.6",
  version      := "0.1.0-SNAPSHOT"
)

lazy val core = (project in file("core")).
  settings(
    inThisBuild(common),
    name := "core",
    libraryDependencies ++= Seq(
      cats,
      catsEffect,
      monix,
      doobie,
      scalaTest % Test
    )
  )

lazy val example = (project in file("example")).
  settings(
    inThisBuild(common),
    name := "example"
  ).dependsOn(core)
