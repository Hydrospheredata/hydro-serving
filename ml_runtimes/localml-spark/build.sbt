import sbt.Keys._
import sbtassembly.Plugin.AssemblyKeys._

def sparkDependencies(v: String) =
  Seq(
    "org.apache.spark" %% "spark-core" % v,
    "org.apache.spark" %% "spark-sql" % v,
    "org.apache.spark" %% "spark-hive" % v,
    "org.apache.spark" %% "spark-streaming" % v,
    "org.apache.spark" %% "spark-mllib" % v
  )

lazy val localMlServe = project.in(file("."))
  .settings(assemblySettings)
  .settings(
    name := "spark-localml-serve",
    version := "1.0",
    scalaVersion := "2.11.8",
    mainClass in assembly := Some("org.kineticcookie.serve.Boot")
  )
  .settings(
    libraryDependencies += "io.hydrosphere" %% "mist-lib" % "0.1.4",
    libraryDependencies ++= sparkDependencies("2.1.0"),
    libraryDependencies ++= {
      val akkaV = "2.4.14"
      val akkaHttpV = "10.0.0"
      Seq(
        "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-jackson" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-xml" % akkaHttpV,
        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "ch.megard" %% "akka-http-cors" % "0.1.10"
      )
    }
  )
  .settings(commonAssemblySettings)

lazy val commonAssemblySettings = Seq(
  mergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
    case PathList("META-INF", "services", "org.apache.hadoop.fs.FileSystem") => MergeStrategy.filterDistinctLines
    case m if m.startsWith("META-INF") => MergeStrategy.discard
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
    case PathList("org", "apache", xs@_*) => MergeStrategy.first
    case PathList("org", "jboss", xs@_*) => MergeStrategy.first
    case "about.html" => MergeStrategy.rename
    case "reference.conf" => MergeStrategy.concat
    case PathList("org", "datanucleus", xs@_*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  test in assembly := {}
)
