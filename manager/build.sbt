import java.io.File
import java.util
import java.util.Collections

import com.spotify.docker.client.messages._
import com.spotify.docker.client._
import sbt.inc.Analysis

import scala.collection.JavaConverters._

name := "manager"

enablePlugins(DockerSpotifyClientPlugin)
enablePlugins(sbtdocker.DockerPlugin)

lazy val uiVersion="0.0.8"

lazy val dataBaseName = "docker"
lazy val dataBaseUser = "docker"
lazy val dataBasePassword = "docker"
lazy val dataBaseUrl = s"jdbc:postgresql://localhost:5432/$dataBaseName"

lazy val uiDownload = taskKey[Unit]("Download and extract UI")

uiDownload := {
  val zipFile=new File(s"${baseDirectory.value}/target/ui/release-$uiVersion.zip")
  val uiArchive=s"https://github.com/Hydrospheredata/hydro-serving-ui/releases/download/$uiVersion/release-$uiVersion.zip"
  val targetDir=new File(s"${classDirectory.in(Compile).value}/ui")

  IO.delete(targetDir)
  IO.createDirectory(targetDir)
  if(!zipFile.exists()){
    IO.download(new URL(uiArchive), zipFile)
  }
  IO.unzip(zipFile, targetDir)
}

lazy val startDatabase = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
  val cli: DockerClient = DefaultDockerClient.fromEnv().build()
  val dbImage="postgres:9.6-alpine"

  cli.listContainers(DockerClient.ListContainersParam.allContainers(true)).asScala
    .filter(p => p.names().contains("/postgres_compile"))
    .foreach(p => cli.removeContainer(p.id(), DockerClient.RemoveContainerParam.forceKill(true)))

  cli.pull(dbImage)
  val containerId = cli.createContainer(ContainerConfig.builder()
    .env(s"POSTGRES_DB=$dataBaseName", s"POSTGRES_USER=$dataBaseUser", s"POSTGRES_PASSWORD=$dataBasePassword")
    .hostConfig(HostConfig.builder()
      .portBindings(Collections.singletonMap("5432",
        util.Arrays.asList(PortBinding.of("0.0.0.0", 5432))))
      .build())
    .exposedPorts("5432")
    .image(dbImage)
    .build(), "postgres_compile").id()
  cli.startContainer(containerId)
  Thread.sleep(10000)
  println(s"starting database...$containerId")
}

compile in Compile <<= (compile in Compile)
  .dependsOn(uiDownload)
  .dependsOn(slickCodeGenTask)
  .dependsOn(flywayMigrate in migration)
  .dependsOn(startDatabase) map { analysis =>
  //Stop database
  val cli: DockerClient = DefaultDockerClient.fromEnv().build()
  cli.listContainers(DockerClient.ListContainersParam.allContainers(true)).asScala
    .filter(p => p.names().contains("/postgres_compile"))
    .foreach(p => cli.removeContainer(p.id(), DockerClient.RemoveContainerParam.forceKill(true)))

  Analysis.Empty
}

lazy val migration = project.settings(
  flywayUrl := dataBaseUrl,
  flywayUser := dataBaseUser,
  flywayPassword := dataBasePassword,
  flywaySchemas := Seq("hydro_serving"),
  flywayLocations := Seq(s"filesystem:${baseDirectory.value}/../src/main/resources/db/migration/"),
  libraryDependencies += "org.postgresql" % "postgresql" % "42.1.3" % "runtime"
)

//This helps to Idea
unmanagedSourceDirectories in Compile += sourceManaged.value

lazy val slickCodeGenTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
  val outputDir = dir.getPath
  val url = dataBaseUrl
  val jdbcDriver = "org.postgresql.Driver"
  val slickDriver = "io.hydrosphere.slick.HydrospherePostgresDriver"
  val pkg = "io.hydrosphere.serving.manager.db"
  toError(r.run("io.hydrosphere.slick.HydrosphereCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, url, outputDir, pkg, dataBaseUser, dataBasePassword), s.log))
  println("Generated")
}



imageNames in docker := Seq(
  ImageName(s"hydrosphere/serving-manager:${version.value}")
)

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val dockerFilesLocation=baseDirectory.value / "src/main/docker/"
  val jarTarget = s"/hydro-serving/app/manager.jar"

  new sbtdocker.Dockerfile {
    // Base image
    from(s"hydro-serving/java:${version.value}")

    env("HS_SERVICE_ID", "-20")

    label("hydroServingServiceId", "-20")
    label("HS_SERVICE_MARKER", "HS_SERVICE_MARKER")
    label("RUNTIME_TYPE_NAME", "hysroserving-java")
    label("RUNTIME_TYPE_VERSION", version.value)
    label("MODEL_NAME", "manager")
    label("MODEL_VERSION", version.value)

    add(dockerFilesLocation, "/hydro-serving/app/")
    // Add all files on the classpath
    add(classpath.files, "/hydro-serving/app/lib/")
    // Add the JAR file
    add(jarFile, jarTarget)
  }
}