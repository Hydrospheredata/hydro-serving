import java.util.{Collections, Properties}

import sbt._
import sbt.Keys._
import com.spotify.docker.client.messages._
import com.spotify.docker.client._
import org.flywaydb.core.Flyway
import sbt.internal.TaskSequential

import scala.collection.JavaConverters._

object SlickGen {

  // workaround for https://github.com/sbt/sbt/issues/3110
  val StableDef = new TaskSequential {}

  lazy val dataBaseName = "docker"
  lazy val dataBaseUser = "docker"
  lazy val dataBasePassword = "docker"
  lazy val dataBaseUrl = s"jdbc:postgresql://localhost:15432/$dataBaseName"

  lazy val startDatabase = taskKey[Unit]("Start database")
  lazy val slickCodeGenTask = taskKey[Unit]("Slick codegen")

  lazy val settings = Seq(
    unmanagedSourceDirectories in Compile += sourceManaged.value,
    startDatabase := {
      val s = streams.value
      startPg(s.log)
    },
    sourceGenerators in Compile += Def.taskDyn[Seq[File]] {
      val location = baseDirectory.value / "src" / "main" / "resources" / "db" / "migration"
      val migrations = location.listFiles()
      val dir = sourceManaged.value / "io" / "hydrosphere" / "serving" / "manager" / "db"
      val f = dir / "Tables.scala"
      val check = dir / "hash.check"
      val cp = (dependencyClasspath in Compile).value.map(_.data)
      val s = streams.value

      def upToDate(): Boolean = {
        if (check.exists()) {
          val data = IO.readLines(check).grouped(2).map(l => l(0) -> l(1)).toSeq
          val real = migrations.map(f => f.getAbsolutePath -> f.lastModified().toString)
          data.zip(real).forall({case (one, two) => one == two})
        } else {
          false
        }
      }

      if (f.exists() && upToDate) Def.task(Seq(f))

      else Def.task {
        StableDef.sequential(
          Def.task(s.log.info("Generating slick tables")),
          startDatabase,
          Def.task(migrate(location, cp)),
          slickCodeGenTask
        ).andFinally {
          stopPg()
        }.value

        val real = migrations.map(f => f.getAbsolutePath -> f.lastModified())
        val out = real.map({ case (n, hash) => n + "\n" + hash }).mkString("\n")
        IO.write(check, out)
        Seq(f)
      }
    },
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
  )


  private def startPg(log: Logger): Unit = {
    log.info(s"starting database")
    val cli: DockerClient = DefaultDockerClient.fromEnv().build()
    val dbImage = "postgres:9.6-alpine"

    cli.listContainers(DockerClient.ListContainersParam.allContainers(true)).asScala
      .filter(p => p.names().contains("/postgres_compile"))
      .foreach(p => cli.removeContainer(p.id(), DockerClient.RemoveContainerParam.forceKill(true)))

    cli.pull(dbImage)
    val containerId = cli.createContainer(ContainerConfig.builder()
      .env(s"POSTGRES_DB=$dataBaseName", s"POSTGRES_USER=$dataBaseUser", s"POSTGRES_PASSWORD=$dataBasePassword")
      .hostConfig(HostConfig.builder()
        .portBindings(Collections.singletonMap("5432", java.util.Arrays.asList(PortBinding.of("0.0.0.0", 15432))))
        .build())
      .exposedPorts("5432")
      .image(dbImage)
      .build(), "postgres_compile").id()
    cli.startContainer(containerId)
    Thread.sleep(10000)
    log.info(s"Database started: $containerId")
  }

  private def stopPg(): Unit = {
    val cli: DockerClient = DefaultDockerClient.fromEnv().build()
    cli.listContainers(DockerClient.ListContainersParam.allContainers(true)).asScala
      .filter(p => p.names().contains("/postgres_compile"))
      .foreach(p => cli.removeContainer(p.id(), DockerClient.RemoveContainerParam.forceKill(true)))
  }

  private def migrate(location: File, cp: Seq[File]): Unit = {
    withContextClassLoader(cp) {
      val flyway = new Flyway()
      val propsOrig = Map(
        "flyway.url" -> "jdbc:postgresql://localhost:15432/docker",
        "flyway.user" -> "docker",
        "flyway.password" -> "docker"
        //  flywayLocations := Seq(s"filesystem:${baseDirectory.value}/../src/main/resources/db/migration/"),
      )
      val props = new Properties()
      System.getProperties.asScala.filter(e => e._1.startsWith("flyway")).foreach(e => props.put(e._1, e._2))
      propsOrig.filter(e => !sys.props.contains(e._1)).foreach(e => props.put(e._1, e._2))

      flyway.setLocations(s"filesystem:$location")
      flyway.configure(props)
      flyway.setSchemas("hydro_serving")
      flyway.migrate()
    }
  }

  private def withContextClassLoader[T](cp: Seq[File])(f: => T): T = {
    val classloader = sbt.internal.inc.classpath.ClasspathUtilities.toLoader(cp, getClass.getClassLoader)
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader
    try {
      thread.setContextClassLoader(classloader)
      f
    } finally {
      thread.setContextClassLoader(oldLoader)
    }
  }
}
