package io.hydrosphere.serving.manager.test

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import io.hydrosphere.serving.manager._
import io.hydrosphere.serving.manager.util.IsolatedDockerClient
import org.scalatest._
import org.testcontainers.containers.wait.Wait

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success, Try}


trait FullIntegrationSpec extends AsyncWordSpecLike
  with ForAllTestContainer with BeforeAndAfterEach {

  implicit val system = ActorSystem("fullIT-system")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.dispatcher
  implicit val timeout = Timeout(5.minute)

  override val container = GenericContainer("postgres:9.6-alpine",
    exposedPorts = Seq(5432),
    env = Map(
      "POSTGRES_DB" -> "docker",
      "POSTGRES_PASSWORD" -> "docker",
      "POSTGRES_USER" -> "docker"
    ),
    waitStrategy = Wait.defaultWaitStrategy()
  )
  container.container.start()

  private[this] var rawConfig = ConfigFactory.load()
  rawConfig = rawConfig.withValue(
    "database",
    rawConfig.getConfig("database").withValue(
      "jdbcUrl",
      ConfigValueFactory.fromAnyRef(s"jdbc:postgresql://localhost:${container.mappedPort(5432)}/docker")
    ).root()
  )

  private[this] val originalConfiguration = ManagerConfiguration.parse(rawConfig)

  val configuration = originalConfiguration

  val dockerClient = IsolatedDockerClient.createFromEnv()
  val managerRepositories = new ManagerRepositoriesConfig(configuration)
  val managerServices = new ManagerServices(managerRepositories, configuration, dockerClient)
  val managerApi = new ManagerHttpApi(managerServices, configuration)
  val managerGRPC = new ManagerGRPC(managerServices, configuration)

  override def run(testName: Option[String], args: Args): Status = {
    try {
      super.run(testName, args)
    } catch {
      case ex: Throwable => throw ex
    } finally {
      dockerClient.close()
      managerGRPC.server.shutdown()
      system.terminate()
    }
  }

  private def getScript(path: String): String = {
    Source.fromInputStream(getClass.getResourceAsStream(path)).mkString
  }

  private def tryCloseable[A <: AutoCloseable, B](a: A)(f: A => B): Try[B] = {
    try {
      Success(f(a))
    } catch {
      case e: Throwable => Failure(e)
    } finally {
      a.close()
    }
  }

  protected def executeDBScript(scripts: String*): Unit = {
    scripts.foreach { sPath =>
      val connection = managerRepositories.dataService.dataSource.getConnection
      val res = tryCloseable(connection) { conn =>
        val st = conn.createStatement()
        tryCloseable(st)(_.execute(getScript(sPath)))
      }
      res.flatten.get
    }
  }
}
