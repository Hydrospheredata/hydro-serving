import sbt._
import Keys._
import com.typesafe.sbt.GitPlugin.autoImport.git
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport.{BuildInfoKey, BuildInfoOption, buildInfoKeys, buildInfoOptions, buildInfoPackage}
import sbtdocker.DockerPlugin.autoImport.{ImageName, dockerfile, imageNames}

object Settings {
  val basicSettings = Seq(
    name := "serving-manager",
    version := sys.props.getOrElse("appVersion", IO.read(file("version")).trim),
    scalaVersion := "2.12.8",
    publishArtifact := false,
    organization := "io.hydrosphere.serving",
    homepage := Some(url("https://hydrosphere.io/serving-docs-new")),
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

  lazy val buildInfoSettings = Seq(
    enablePlugins(BuildInfoPlugin),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitCurrentBranch, git.gitCurrentTags, git.gitHeadCommit),
    buildInfoPackage := "io.hydrosphere.serving",
    buildInfoOptions += BuildInfoOption.ToJson
  )

  lazy val dockerSettings = Seq(
    enablePlugins(sbtdocker.DockerPlugin),
    imageNames in docker := Seq(ImageName(s"hydrosphere/serving-manager:${version.value}")),
    dockerfile in docker := {
      val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
      val classpath = (dependencyClasspath in Compile).value
      val dockerFilesLocation = baseDirectory.value / "src/main/docker/"
      val jarTarget = s"/hydro-serving/app/manager.jar"
      val osName = sys.props.get("os.name").getOrElse("unknown")

      new sbtdocker.Dockerfile {
        // Base image
        from("openjdk:8u151-jre-alpine")

        run("apk", "update")
        run("apk", "add", "jq")
        run("rm", "-rf", "/var/cache/apk/*")

        add(dockerFilesLocation, "/hydro-serving/app/")
        // Add all files on the classpath
        add(classpath.files, "/hydro-serving/app/lib/")
        // Add the JAR file
        add(jarFile, jarTarget)

        cmd("/hydro-serving/app/start.sh")
      }
    }
  )

  val all: Seq[Def.Setting[_]] = basicSettings ++ testSettings ++ buildInfoSettings ++ dockerSettings
}