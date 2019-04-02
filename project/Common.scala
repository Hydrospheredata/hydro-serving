import sbt._
import Keys._

object Common {

  val scalaVer = "2.12.8"

  val testSettings = Seq(
    parallelExecution in Test := false,
    parallelExecution in IntegrationTest := false,

    fork in(Test, test) := true,
    fork in(IntegrationTest, test) := true,
    fork in(IntegrationTest, testOnly) := true
  )

  lazy val currentAppVersion = sys.props.getOrElse("appVersion", IO.read(file("version")).trim)

  val settings: Seq[Def.Setting[_]] = Seq(
    version := currentAppVersion,
    scalaVersion := scalaVer,
    publishArtifact := false,
    organization := "io.hydrosphere.serving",
    homepage := Some(url("https://hydrosphere.io/serving-docs")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-Ypartial-unification"
    )
  ) ++ testSettings
}