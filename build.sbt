import Dependencies._
import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runClean, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upsteam branch is properly configured
)

lazy val scala213 = "2.13.16"

ThisBuild / scalaVersion := scala213
ThisBuild / organization := "com.yarhrn"
ThisBuild / homepage := Some(url("https://github.com/yarhrn/loco"))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/yarhrn/loco"), "git@github.com:yarhrn/loco.git"))
ThisBuild / developers := List(
  Developer("Yaroslav Hryniuk", "Yaroslav Hryniuk", "yaroslavh.hryniuk@gmail.com", url("https://github.com/yarhrn")))
ThisBuild / licenses += ("MIT", url("https://github.com/yarhrn/loco/blob/master/LICENSE"))
ThisBuild / publishMavenStyle := true
releaseTagName := s"${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"

lazy val loco = project
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true,
  )
  .in(file("."))
  .aggregate(
    core,
    doobie,
    example
  )

lazy val core = (project in file("core")).settings(
  name := "loco-core",
  libraryDependencies ++= Seq(
    scalaTest,
    scalaMock,
    fs2Core,
    catsEffect,
    jsoniter,
    jsoniterMacros,
    catsEffectStd
  ),
)

lazy val example = (project in file("example"))
  .settings(
    name := "loco-example",
    libraryDependencies ++= Seq(scalaTest, scalaMock),
    publish / skip := true
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val doobie = (project in file("doobie"))
  .settings(
    name := "loco-doobie",
    libraryDependencies ++= Seq(doobieCore, scalaTest, scalaMock, postgresql, embeddedPostgresql),
  )
  .dependsOn(core % "test->test;compile->compile")
