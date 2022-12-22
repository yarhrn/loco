import Dependencies._
import ReleaseTransformations._

name := "loco"


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
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)

val common = List(
  scalaVersion := "2.13.6",
  scalacOptions ++= List(
    "-Wvalue-discard"
  )
)
inThisBuild(common)


val publishing = List(
  organization := "com.yarhrn",
  homepage := Some(url("https://github.com/yarhrn/simple-scala-json-rpc")),
  scmInfo := Some(ScmInfo(url("https://github.com/yarhrn/simple-scala-json-rpc"), "git@github.com:yarhrn/simple-scala-json-rpc.git")),
  developers := List(Developer("Yaroslav Hryniuk",
    "Yaroslav Hryniuk",
    "yaroslavh.hryniuk@gmail.com",
    url("https://github.com/yarhrn"))),
  licenses += ("MIT", url("https://github.com/yarhrn/simple-scala-json-rpc/blob/main/LICENSE")),
  publishMavenStyle := true
)



lazy val core = (project in file("core"))
  .settings(
    publishing,
    name := "core",
    libraryDependencies ++= Seq(
      scalaTest,
      scalaMock,
      fs2Core,
      catsEffect,
      jsoniter,
      jsoniterMacros,
      catsEffectStd
    ),
    publishing
  )

//
//lazy val example = (project in file("example")).
//  settings(
//    publishing,
//    name := "example",
//    libraryDependencies ++= Seq(scalaTest, scalaMock)
//  ).dependsOn(core % "test->test;compile->compile")
//

lazy val doobie = (project in file("doobie"))
  .settings(
    publishing,
    name := "doobie",
    libraryDependencies ++= Seq(doobieCore, scalaTest, scalaMock, postgresql, embeddedPostgresql)
  ).dependsOn(core)