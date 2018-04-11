import sbt._
import Keys._

object Common {

  val scalaVer = "2.11.11"

  val testSettings = Seq(
    parallelExecution in Test := false,
    parallelExecution in IntegrationTest := false,

    fork in(Test, test) := true,
    fork in(IntegrationTest, test) := true,
    fork in(IntegrationTest, testOnly) := true
  )

  val settings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := scalaVer,
    publishArtifact := false,
    organization := "io.hydrosphere.serving",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-Ypartial-unification"
    ),
    resolvers += Resolver.bintrayRepo("findify", "maven")
  ) ++ testSettings
}

