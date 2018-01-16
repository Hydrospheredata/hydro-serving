import sbt.Keys._
import sbt._

import scala.sys.process.Process

name := "sidecar"

lazy val buildSidecar = inputKey[Unit]("Build script")

buildSidecar := {
  val args = Seq("./build_all.sh", version.value)
  val home = baseDirectory.value

  val ps = Process(args, Some(home / "/"))
  if (ps.!(streams.value.log) != 0) {
    throw new IllegalStateException("Wrong result code from sidecar build")
  }
}
