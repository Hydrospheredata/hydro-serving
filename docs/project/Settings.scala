import sbt._
import Keys._

object Settings {
  val basicSettings = Seq(
    name := "Hydrosphere Serving documentation",
    version := sys.props.getOrElse("appVersion", IO.read(file("../version")).trim),
    scalaVersion := "2.12.8",
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
  )
  val testSettings = Seq(
    parallelExecution in Test := false,
    parallelExecution in IntegrationTest := false,

    fork in(Test, test) := true,
    fork in(IntegrationTest, test) := true,
    fork in(IntegrationTest, testOnly) := true
  )

  val all: Seq[Def.Setting[_]] = basicSettings ++ testSettings
}
