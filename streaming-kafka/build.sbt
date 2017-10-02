name := "streaming-kafka"

enablePlugins(sbtdocker.DockerPlugin)

imageNames in docker := Seq(
  ImageName(s"hydrosphere/serving-streaming-kafka:${version.value}")
)

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val dockerFilesLocation=baseDirectory.value / "src/main/docker/"
  val jarTarget = s"/hydro-serving/app/streaming-kafka.jar"

  new sbtdocker.Dockerfile {
    // Base image
    from(s"hydro-serving/java:${version.value}")

    label("HS_SERVICE_MARKER", "HS_SERVICE_MARKER")
    label("RUNTIME_TYPE_NAME", "hysroserving-java")
    label("RUNTIME_TYPE_VERSION", version.value)
    label("MODEL_NAME", "streaming-kafka")
    label("MODEL_VERSION", version.value)

    add(dockerFilesLocation, "/hydro-serving/app/")
    // Add all files on the classpath
    add(classpath.files, "/hydro-serving/app/lib/")
    // Add the JAR file
    add(jarFile, jarTarget)
  }
}