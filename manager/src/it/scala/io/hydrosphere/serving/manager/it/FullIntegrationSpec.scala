package io.hydrosphere.serving.manager.it

import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import io.hydrosphere.serving.manager._
import io.hydrosphere.serving.manager.model.ModelBuildStatus
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service.runtime.DefaultRuntimes
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success, Try}


trait FullIntegrationSpec extends DatabaseAccessIT
  with BeforeAndAfterEach {

  implicit val system = ActorSystem("fullIT-system")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.dispatcher
  implicit val timeout = Timeout(5.minute)

  private[this] var rawConfig = ConfigFactory.load()
  rawConfig = rawConfig.withValue(
    "database",
    rawConfig.getConfig("database").withValue(
      "jdbcUrl",
      ConfigValueFactory.fromAnyRef(s"jdbc:postgresql://localhost:5432/docker")
    ).root()
  )

  private[this] val originalConfiguration = ManagerConfiguration.parse(rawConfig)

  def configuration = originalConfiguration.copy(runtimesStarterPack = DefaultRuntimes.dummies)

  var managerRepositories: ManagerRepositories = _
  var managerServices: ManagerServices = _
  var managerApi: ManagerHttpApi = _
  var managerGRPC: ManagerGRPC = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    managerRepositories = new ManagerRepositories(configuration)
    managerServices = new ManagerServices(managerRepositories, configuration, dockerClient)
    managerApi = new ManagerHttpApi(managerServices, configuration)
    managerGRPC = new ManagerGRPC(managerServices, configuration)
  }

  override def afterAll(): Unit = {
    managerGRPC.server.shutdown()
    system.terminate()
    super.afterAll()
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

  protected def packModel(str: String): Path = {
    val temptar = Files.createTempFile("packedModel", ".tar.gz")
    TarGzUtils.compressFolder(Paths.get(getClass.getResource(str).getPath), temptar)
    temptar
  }

  protected def awaitBuild(modelBuildId: Long)(implicit timeout: Duration): ModelBuild = {
    var result = Option.empty[ModelBuild]

    while (result.isEmpty) {
      val buildOptF = managerRepositories.modelBuildRepository.get(modelBuildId)
      val buildOpt = Await.result(buildOptF, timeout)
      buildOpt.get.status match {
        case ModelBuildStatus.FINISHED => result = buildOpt
        case _ => Unit
      }
    }
    assert(result.isDefined)
    result.get
  }

  protected def awaitVersion(modelBuildId: Long)(implicit timeout: Duration): ModelVersion = {
    val finishedBuild = awaitBuild(modelBuildId)
    val versionOpt = finishedBuild.modelVersion
    assert(versionOpt.isDefined)
    versionOpt.get
  }
}
