package io.hydrosphere.serving.manager.test

import java.time.Duration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import io.hydrosphere.serving.manager.service.clouddriver.RuntimeDeployService
import io.hydrosphere.serving.manager.service.modelbuild.{ModelBuildService, ModelPushService}
import io.hydrosphere.serving.manager.{ManagerConfiguration, ManagerRepositoriesConfig, ManagerServices}
import org.mockito
import org.mockito.Mockito
import org.scalactic.source.Position
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FunSpec, FunSpecLike}
import org.testcontainers.containers.wait.Wait

import scala.io.Source

/**
  *
  */
class CommonIntegrationSpec extends TestKit(ActorSystem("testMasterService"))
  with FunSpecLike with ForAllTestContainer with MockitoSugar with BeforeAndAfterEach {

  implicit val materializer = ActorMaterializer()
  implicit val ex = system.dispatcher


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

  /*Mocks*/
  val mockModelBuildService = mock[ModelBuildService]
  val mockModelPushService = mock[ModelPushService]
  val mockDockerClient = mock[DockerClient]
  val mockRuntimeDeployService = mock[RuntimeDeployService]


  override protected def beforeEach(): Unit = {
    mockito.Mockito.reset(mockModelPushService)
    mockito.Mockito.reset(mockModelBuildService)
    mockito.Mockito.reset(mockRuntimeDeployService)
    mockito.Mockito.reset(mockDockerClient)

    super.beforeEach()
  }

  val originalConfiguration = ManagerConfiguration.parse(ConfigFactory.load())
  val configuration = originalConfiguration.copy(database =
    originalConfiguration.database.withValue(
      "jdbcUrl",
      ConfigValueFactory.fromAnyRef(s"jdbc:postgresql://localhost:${container.mappedPort(5432)}/docker"))
  )

  val managerRepositories = new ManagerRepositoriesConfig(configuration)
  val managerServices = new ManagerServices(managerRepositories, configuration) {

    override val runtimeDeployService: RuntimeDeployService = mockRuntimeDeployService

    override val dockerClient: DockerClient = mockDockerClient

    override val modelBuildService: ModelBuildService = mockModelBuildService

    override val modelPushService: ModelPushService = mockModelPushService
  }

  private def getScript(path: String): String = {
    Source.fromInputStream(getClass.getResourceAsStream(path)).mkString
  }

  protected def executeDBScript(scripts: String*): Unit = {
    scripts.foreach(sPath => {
      val connection = managerRepositories.dataService.dataSource.getConnection
      try {
        val statement = connection.createStatement()
        try {
          statement.execute(getScript(sPath))
        } catch {
          case stEx: Throwable => throw stEx
        } finally {
          statement.close()
        }
      } catch {
        case e: Throwable => throw e
      } finally {
        connection.close()
      }
    })
  }
}
