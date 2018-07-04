package io.hydrosphere.serving.manager.service.runtime

import java.util.concurrent.Executors

import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{CreateRuntimeRequest, PullRuntime, Runtime}
import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.repository.{RuntimePullRepository, RuntimeRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class RuntimeManagementServiceImpl(
  runtimeRepository: RuntimeRepository,
  runtimePullRepository: RuntimePullRepository,
  dockerClient: DockerClient
)(
  implicit ex: ExecutionContext
) extends RuntimeManagementService with Logging {
  val dockerPullExecutor = new RuntimePullExecutor(
    runtimePullRepository,
    runtimeRepository,
    dockerClient,
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  )

  override def lookupByModelType(modelTypes: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByModelType(
      modelTypes.map(ModelType.fromTag).toSeq
    )

  override def lookupByTag(tags: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByTags(tags.toSeq)

  override def all(): Future[Seq[Runtime]] =
    runtimeRepository.all()

  override def create(request: CreateRuntimeRequest): HFResult[PullRuntime] = {
    logger.debug(request)
    runtimeRepository.fetchByNameAndVersion(request.name, request.version).flatMap {
      case Some(_) =>
        logger.warn(s"Tried to add already existing runtime ${request.fullImage}")
        Result.clientErrorF(s"Runtime ${request.fullImage} already exists")
      case None =>
        runtimePullRepository.getRunningPull(request.name, request.version).flatMap {
          case Some(_) =>
            Result.clientErrorF(s"Image ${request.fullImage} is already creating")
          case None =>
            val task = dockerPullExecutor.execute(request)
            task.future.foreach { runtime =>
              logger.info(s"Runtime added: $runtime")
            }
            task.taskStatus.map { status =>
              Result.ok(PullRuntime.fromServiceTask(status))
            }
        }
    }
  }

  override def get(id: Long): HFResult[Runtime] =
    runtimeRepository.get(id).map(_.toHResult(ClientError(s"Can't find Runtime with id $id")))

  override def getPullStatus(requestId: Long): HFResult[PullRuntime] = {
    runtimePullRepository.get(requestId).map(_.toHResult(ClientError(s"Can't find pull with id: ${requestId.toString}")))
  }
}