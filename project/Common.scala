import sbt._
import Keys._

object Common {
  val appVersion = "0.0.1"

  val settings: Seq[Def.Setting[_]] = Seq(
    version := appVersion,
    scalaVersion := "2.11.11",
    //crossScalaVersions := Seq("2.11.11"),
    publishArtifact := false,
    exportJars := true,
    organization := "io.hydrosphere.serving",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
    /*resolvers ++= {
      // Only add Sonatype Snapshots if this version itself is a snapshot version
      if(isSnapshot.value) {
        Seq("Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      } else {
        Seq()
      }
    }*/
  )
}

