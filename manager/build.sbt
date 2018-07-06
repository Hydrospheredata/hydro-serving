import java.io.File
import java.util
import java.util.Collections

import com.spotify.docker.client.messages._
import com.spotify.docker.client._

import scala.collection.JavaConverters._

name := "manager"

enablePlugins(DockerSpotifyClientPlugin)
enablePlugins(sbtdocker.DockerPlugin)

lazy val dataBaseName = "docker"
lazy val dataBaseUser = "docker"
lazy val dataBasePassword = "docker"
lazy val dataBaseUrl = s"jdbc:postgresql://localhost:15432/$dataBaseName"

lazy val startDatabase = taskKey[Unit]("Start database")
lazy val slickCodeGenTask = taskKey[Unit]("Slick codegen")

startDatabase := {
  val dir = sourceManaged
  val cp =  dependencyClasspath in Compile
  val r = runner in Compile
  val s = streams

  val cli: DockerClient = DefaultDockerClient.fromEnv().build()
  val dbImage = "postgres:9.6-alpine"

  cli.listContainers(DockerClient.ListContainersParam.allContainers(true)).asScala
    .filter(p => p.names().contains("/postgres_compile"))
    .foreach(p => cli.removeContainer(p.id(), DockerClient.RemoveContainerParam.forceKill(true)))

  cli.pull(dbImage)
  val containerId = cli.createContainer(ContainerConfig.builder()
    .env(s"POSTGRES_DB=$dataBaseName", s"POSTGRES_USER=$dataBaseUser", s"POSTGRES_PASSWORD=$dataBasePassword")
    .hostConfig(HostConfig.builder()
      .portBindings(Collections.singletonMap("5432",
        util.Arrays.asList(PortBinding.of("0.0.0.0", 15432))))
      .build())
    .exposedPorts("5432")
    .image(dbImage)
    .build(), "postgres_compile").id()
  cli.startContainer(containerId)
  Thread.sleep(10000)
  println(s"starting database...$containerId")
}

compile in Compile := (compile in Compile)
  .dependsOn(slickCodeGenTask)
  .dependsOn(flywayMigrate in migration)
  .dependsOn(startDatabase)
  .andFinally{
  //Stop database
  val cli: DockerClient = DefaultDockerClient.fromEnv().build()
  cli.listContainers(DockerClient.ListContainersParam.allContainers(true)).asScala
    .filter(p => p.names().contains("/postgres_compile"))
    .foreach(p => cli.removeContainer(p.id(), DockerClient.RemoveContainerParam.forceKill(true)))
  }.value

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

slickCodeGenTask := {
  val dir = sourceManaged.value
  val cp = (dependencyClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value
  val outputDir = dir.getPath

  val url = dataBaseUrl
  val jdbcDriver = "org.postgresql.Driver"
  val slickDriver = "io.hydrosphere.slick.HydrospherePostgresDriver"
  val pkg = "io.hydrosphere.serving.manager.db"
  val runResult = r.run(
    "io.hydrosphere.slick.HydrosphereCodeGenerator",
    cp.files,
    Array(slickDriver, jdbcDriver, url, outputDir, pkg, dataBaseUser, dataBasePassword),
    s.log
  )
  runResult.recover{
    case err: Throwable => sys.error(err.getMessage)
  }
  println("Generated")
}

imageNames in docker := Seq(
  ImageName(s"hydrosphere/serving-manager:${version.value}")
)

dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val dockerFilesLocation = baseDirectory.value / "src/main/docker/"
  val jarTarget = s"/hydro-serving/app/manager.jar"

  new sbtdocker.Dockerfile {
    // Base image
    from("openjdk:8u151-jre-alpine")

    run("apk", "update")
    run("apk", "add", "jq")
    run("rm", "-rf", "/var/cache/apk/*")

    label("SERVICE_ID", "-20")
    label("HS_SERVICE_MARKER", "HS_SERVICE_MARKER")
    label("DEPLOYMENT_TYPE", "APP")
    label("RUNTIME_ID", "-20")
    label("SERVICE_NAME", "manager")


    add(dockerFilesLocation, "/hydro-serving/app/")
    // Add all files on the classpath
    add(classpath.files, "/hydro-serving/app/lib/")
    // Add the JAR file
    add(jarFile, jarTarget)
    cmd("/hydro-serving/app/start.sh")
  }
}
