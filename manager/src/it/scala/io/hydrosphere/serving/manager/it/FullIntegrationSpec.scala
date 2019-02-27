package io.hydrosphere.serving.manager.it

import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.data.EitherT
import cats.effect.IO
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import io.grpc.Server
import io.hydrosphere.serving.manager._
import io.hydrosphere.serving.manager.api.grpc.GrpcApiServer
import io.hydrosphere.serving.manager.api.http.HttpApiServer
import io.hydrosphere.serving.manager.config.{DockerClientConfig, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


trait FullIntegrationSpec extends DatabaseAccessIT
  with BeforeAndAfterEach {

  implicit val system = ActorSystem("fullIT-system")
  implicit val materializer = ActorMaterializer()
  implicit val ex = ExecutionContext.global
  implicit val contextShift = IO.contextShift(ex)
  implicit val timeout = Timeout(5.minute)

  val dummyImage = DockerImage(
    name = "hydrosphere/serving-runtime-dummy",
    tag = "latest"
  )

  private[this] var rawConfig = ConfigFactory.load()
  rawConfig = rawConfig.withValue(
    "database",
    rawConfig.getConfig("database").withValue(
      "jdbcUrl",
      ConfigValueFactory.fromAnyRef(s"jdbc:postgresql://localhost:5432/docker")
    ).root()
  )

  private[this] val originalConfiguration = ManagerConfiguration.load

  def configuration = originalConfiguration.right.get

  var managerRepositories: ManagerRepositories[IO] = _
  var managerServices: ManagerServices[IO] = _
  var managerApi: HttpApiServer[IO] = _
  var managerGRPC: Server = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    managerRepositories = new ManagerRepositories(configuration)
    managerServices = new ManagerServices(managerRepositories, configuration, dockerClient, DockerClientConfig())
    managerApi = new HttpApiServer(managerRepositories, managerServices, configuration)
    managerGRPC = GrpcApiServer(managerRepositories, managerServices, configuration)
    managerApi.start()
    managerGRPC.start()
  }

  override def afterAll(): Unit = {
    managerGRPC.shutdown()
    system.terminate()
    super.afterAll()
  }

  protected def packModel(str: String): Path = {
    val temptar = Files.createTempFile("packedModel", ".tar.gz")
    TarGzUtils.compressFolder(Paths.get(getClass.getResource(str).toURI), temptar)
    temptar
  }

  protected def eitherAssert(body: => IO[Either[DomainError, Assertion]]): Future[Assertion] = {
    body.map {
      case Left(err) =>
        fail(err.message)
      case Right(asserts) =>
        asserts
    }.unsafeToFuture()
  }

  protected def eitherTAssert(body: => EitherT[IO, DomainError, Assertion]): Future[Assertion] = {
    eitherAssert(body.value)
  }

  protected def ioAssert(body: => IO[Assertion]): Future[Assertion] = {
    body.unsafeToFuture()
  }
}