import java.io.File

enablePlugins(sbtdocker.DockerPlugin)

name := "dummy-runtime"

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val dockerFilesLocation=baseDirectory.value / "src/main/docker/"
  val jarTarget = s"/hydro-serving/app/app.jar"

  new Dockerfile {
    from("openjdk:8u151-jre-alpine")

    env("APP_PORT","9090")
    env("SIDECAR_PORT","8080")
    env("SIDECAR_HOST","localhost")
    env("MODEL_DIR","/models")

    add(dockerFilesLocation, "/hydro-serving/app/")
    // Add all files on the classpath
    add(classpath.files, "/hydro-serving/app/lib/")
    // Add the JAR file
    add(jarFile, jarTarget)

    volume("/models")

    cmd("/hydro-serving/app/start.sh")
  }
}

imageNames in docker := Seq(
  ImageName("hydrosphere/dummy-runtime:latest")
)