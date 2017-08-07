import sbt.Keys._
import sbt._

name := "sidecar"

lazy val skipSidecarBuild = util.Properties.propOrElse("skipSidecarBuild", "false")

lazy val execScript = inputKey[Unit]("Build script")
lazy val skipScript = inputKey[Unit]("Skip script")

execScript := {
  val args = Seq("./build_all.sh", version.value)
  val home = baseDirectory.value
  val ps = Process(args, Some(home / "/"))
  val result=ps.!(streams.value.log)
  if (result != 0) {
    println(s"Result code from build script: $result")
    throw new IllegalStateException("Wrong result code from sidecar build")
  }
}

skipScript := {
  println("Skip Sidecar Build")
}

lazy val someScript = Def.taskDyn({
  if ("true".equalsIgnoreCase(skipSidecarBuild))
    skipScript.toTask("")
  else
    execScript.toTask("")
})

(compile in Compile) <<= (compile in Compile).dependsOn(someScript)