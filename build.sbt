import Dependencies._
import ReleaseTransformations._

Global / onChangedBuildSource := ReloadOnSourceChanges


name := "loco"

organization := "com.yarhrn"
homepage := Some(url("https://github.com/yarhrn/loco"))
scmInfo := Some(ScmInfo(url("https://github.com/yarhrn/loco"), "git@github.com:yarhrn/loco.git"))
developers := List(Developer("Yaroslav Hryniuk",
  "Yaroslav Hryniuk",
  "yaroslavh.hryniuk@gmail.com",
  url("https://github.com/yarhrn")))
licenses += ("MIT", url("https://github.com/yarhrn/loco/blob/master/LICENSE"))
publishMavenStyle := true

scalaVersion := "2.13.8"


libraryDependencies ++= Seq(
  scalaTest,
  scalaMock,
  fs2Core,
  catsEffect,
  jsoniter,
  jsoniterMacros,
  catsEffectStd,
  doobieCore, scalaTest, scalaMock, postgresql, embeddedPostgresql
)


releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runClean,                               // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)