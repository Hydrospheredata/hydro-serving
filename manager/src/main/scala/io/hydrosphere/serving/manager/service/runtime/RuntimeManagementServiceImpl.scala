package io.hydrosphere.serving.manager.service.runtime

import java.time.LocalDateTime
import java.util.UUID

import com.amazonaws.services.ecr.model.ImageNotFoundException
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.exceptions.DockerException
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import io.hydrosphere.serving.manager.util.docker.HistoricProgressHandler
import org.apache.logging.log4j.scala.Logging

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RuntimeManagementServiceImpl(
  runtimeRepository: RuntimeRepository,
  dockerClient: DockerClient
)(
  implicit ex: ExecutionContext
) extends RuntimeManagementService with Logging {

  val tasks = TrieMap.empty[UUID, RuntimeCreateStatus]

  override def lookupByModelType(modelTypes: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByModelType(
      modelTypes.map(ModelType.fromTag).toSeq
    )

  override def lookupByTag(tags: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByTags(tags.toSeq)

  override def all(): Future[Seq[Runtime]] =
    runtimeRepository.all()

  override def create(request: CreateRuntimeRequest): HFResult[RuntimeCreationRunning] = {
    logger.debug(request)
    runtimeRepository.fetchByNameAndVersion(request.name, request.version).flatMap {
      case Some(_) =>
        logger.warn(s"Tried to add already existing runtime ${request.fullImage}")
        Result.clientErrorF(s"Runtime ${request.fullImage} already exists")
      case None =>
        val registeredRequest = register(request)
        logger.debug(s"[${registeredRequest.id}] Registered: $registeredRequest")
        Future {
          val logHandler = new HistoricProgressHandler()
          try {
            logger.info(s"[${registeredRequest.id}] Start docker pull ${registeredRequest.request.fullImage}")
            val request = registeredRequest.request
            dockerClient.pull(request.fullImage, logHandler)
            val rType = Runtime(
              id = 0,
              name = request.name,
              version = request.version,
              suitableModelType = request.modelTypes,
              tags = request.tags,
              configParams = request.configParams
            )
            runtimeRepository.create(rType).map { runtime =>
              tasks += registeredRequest.id -> RuntimeCreated(request, registeredRequest.id, registeredRequest.startedAt, LocalDateTime.now(), rType.toImageDef)
              logger.info(s"[${registeredRequest.id}] Runtime added: $runtime")
            }
          } catch {
            case NonFatal(err) =>
              val newFailStatus = err match {
                case ex: ImageNotFoundException =>
                  RuntimeCreationFailed(
                    registeredRequest.request,
                    registeredRequest.id,
                    registeredRequest.startedAt,
                    s"Couldn't find an image ${registeredRequest.request.fullImage}",
                    Some(ex)
                  )

                case ex: DockerException =>
                  RuntimeCreationFailed(
                    registeredRequest.request,
                    registeredRequest.id,
                    registeredRequest.startedAt,
                    s"Internal docker error",
                    Some(ex)
                  )

                case e =>
                  RuntimeCreationFailed(
                    registeredRequest.request,
                    registeredRequest.id,
                    registeredRequest.startedAt,
                    s"Unexpected exception",
                    Some(e)
                  )
              }

              // TODO cleanup after fail?

              tasks.update(registeredRequest.id, newFailStatus)
              logger.warn(s"[${registeredRequest.id}] Docker pull failed: $newFailStatus")
              logger.warn(s"[${registeredRequest.id}] Docker pull logs: ${logHandler.messages.mkString("\n")}")
          }
        }
        Result.okF(registeredRequest)
    }
  }

  override def get(id: Long): HFResult[Runtime] =
    runtimeRepository.get(id).map(_.toHResult(ClientError(s"Can't find Runtime with id $id")))

  override def getCreationStatus(requestId: UUID): HFResult[RuntimeCreateStatus] = {
    Future.successful {
      val res = tasks.get(requestId).toHResult(ClientError(s"Can't find request ${requestId.toString}"))
      logger.debug(s"Status for runtime creation request $requestId = $res")
      res
    }
  }

  def register(request: CreateRuntimeRequest) = {
    val startedAt = LocalDateTime.now()
    val id = UUID.randomUUID()
    val status = RuntimeCreationRunning(id, startedAt, request)
    tasks += id -> status
    status
  }

}