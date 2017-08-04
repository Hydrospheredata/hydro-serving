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
  if (ps.!(streams.value.log) != 0) {
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