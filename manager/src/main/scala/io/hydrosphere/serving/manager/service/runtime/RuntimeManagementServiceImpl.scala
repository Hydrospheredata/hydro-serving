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
import io.hydrosphere.serving.manager.model.{HFResult, HResult, Result}
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import io.hydrosphere.serving.manager.service._
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

  val createTasks = TrieMap.empty[UUID, ServiceTask[CreateRuntimeRequest, Runtime]]
  val syncTasks = TrieMap.empty[UUID, ServiceTask[SyncRuntimeRequest, Runtime]]

  override def lookupByModelType(modelTypes: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByModelType(
      modelTypes.map(ModelType.fromTag).toSeq
    )

  override def lookupByTag(tags: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByTags(tags.toSeq)

  override def all(): Future[Seq[Runtime]] =
    runtimeRepository.all()

  override def create(request: CreateRuntimeRequest): HFResult[ServiceTaskRunning[CreateRuntimeRequest, Runtime]] = {
    logger.debug(request)
    runtimeRepository.fetchByNameAndVersion(request.name, request.version).flatMap {
      case Some(_) =>
        logger.warn(s"Tried to add already existing runtime ${request.fullImage}")
        Result.clientErrorF(s"Runtime ${request.fullImage} already exists")
      case None =>
        Future {
          registerCreate(request).right.map { registeredRequest =>
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
                  suitableModelType = request.modelTypes.map(ModelType.fromTag),
                  tags = request.tags,
                  configParams = request.configParams
                )
                runtimeRepository.create(rType).map { runtime =>
                  createTasks += registeredRequest.id -> ServiceTaskFinished(request, registeredRequest.id, registeredRequest.startedAt, LocalDateTime.now(), rType)
                  logger.info(s"[${registeredRequest.id}] Runtime added: $runtime")
                }
              } catch {
                case NonFatal(err) =>
                  val newFailStatus = err match {
                    case ex: ImageNotFoundException =>
                      ServiceTaskFailed[CreateRuntimeRequest, Runtime](
                        registeredRequest.request,
                        registeredRequest.id,
                        registeredRequest.startedAt,
                        s"Couldn't find an image ${registeredRequest.request.fullImage}",
                        Some(ex)
                      )

                    case ex: DockerException =>
                      ServiceTaskFailed[CreateRuntimeRequest, Runtime](
                        registeredRequest.request,
                        registeredRequest.id,
                        registeredRequest.startedAt,
                        s"Internal docker error",
                        Some(ex)
                      )

                    case e =>
                      ServiceTaskFailed[CreateRuntimeRequest, Runtime](
                        registeredRequest.request,
                        registeredRequest.id,
                        registeredRequest.startedAt,
                        s"Unexpected exception",
                        Some(e)
                      )
                  }

                  // TODO cleanup after fail?

                  createTasks.update(registeredRequest.id, newFailStatus)
                  logger.warn(s"[${registeredRequest.id}] Docker pull failed: $newFailStatus")
                  logger.warn(s"[${registeredRequest.id}] Docker pull logs: ${logHandler.messages.mkString("\n")}")
              }
            }
            registeredRequest
          }
        }
    }
  }

  override def get(id: Long): HFResult[Runtime] =
    runtimeRepository.get(id).map(_.toHResult(ClientError(s"Can't find Runtime with id $id")))

  override def getCreationStatus(requestId: UUID): HFResult[ServiceTask[CreateRuntimeRequest, Runtime]] = {
    Future.successful {
      val res = createTasks.get(requestId).toHResult(ClientError(s"Can't find request ${requestId.toString}"))
      logger.debug(s"Status for runtime creation request $requestId = $res")
      res
    }
  }

  def registerCreate(request: CreateRuntimeRequest): HResult[ServiceTaskRunning[CreateRuntimeRequest, Runtime]] = {
    createTasks.find(x => x._2.request.fullImage == request.fullImage && x._2.status == ServiceTaskStatus.Running) match {
      case Some((key, value)) =>
        Result.clientError(s"For image ${value.request.fullImage} there is already a task $key")
      case None =>
        val startedAt = LocalDateTime.now()
        val id = UUID.randomUUID()
        val status = ServiceTaskRunning[CreateRuntimeRequest, Runtime](id, startedAt, request)
        createTasks += id -> status
        Result.ok(status)
    }
  }

  def registerSync(request: SyncRuntimeRequest): HResult[ServiceTaskRunning[SyncRuntimeRequest, Runtime]] = {
    syncTasks.find(x => x._2.request.runtimeId == request.runtimeId && x._2.status == ServiceTaskStatus.Running) match {
      case Some((key, value)) =>
        Result.clientError(s"For runtime id=${value.request.runtimeId} there is already a task $key")
      case None =>
        val startedAt = LocalDateTime.now()
        val id = UUID.randomUUID()
        val status = ServiceTaskRunning[SyncRuntimeRequest, Runtime](id, startedAt, request)
        syncTasks += id -> status
        Result.ok(status)
    }
  }

  override def sync(request: SyncRuntimeRequest): HFResult[ServiceTaskRunning[SyncRuntimeRequest, Runtime]] = {
    logger.debug(request)
    get(request.runtimeId).map { getResult =>
      getResult.right.flatMap { runtime =>
        sync(runtime)
      }
    }
  }

  override def syncAll(): HFResult[Seq[ServiceTaskRunning[SyncRuntimeRequest, Runtime]]] = {
    runtimeRepository.all().map { runtimes =>
      Result.traverse(runtimes) { runtime =>
        sync(runtime)
      }
    }
  }

  def sync(runtime: Runtime): HResult[ServiceTaskRunning[SyncRuntimeRequest, Runtime]] = {
    val request = SyncRuntimeRequest(runtime.id)
    registerSync(request).right.map { registeredRequest =>
      logger.debug(s"[${registeredRequest.id}] Registered: $registeredRequest")
      Future {
        val logHandler = new HistoricProgressHandler()
        try {
          logger.info(s"[${registeredRequest.id}] Start docker pull ${runtime.toImageDef}")
          val request = registeredRequest.request
          dockerClient.pull(runtime.toImageDef, logHandler)

          syncTasks += registeredRequest.id -> ServiceTaskFinished(request, registeredRequest.id, registeredRequest.startedAt, LocalDateTime.now(), runtime)
          logger.info(s"[${registeredRequest.id}] Docker pull finished ${runtime.toImageDef}")
        } catch {
          case NonFatal(err) =>
            val newFailStatus = err match {
              case ex: ImageNotFoundException =>
                ServiceTaskFailed[SyncRuntimeRequest, Runtime](
                  registeredRequest.request,
                  registeredRequest.id,
                  registeredRequest.startedAt,
                  s"Couldn't find an image ${runtime.toImageDef}",
                  Some(ex)
                )

              case ex: DockerException =>
                ServiceTaskFailed[SyncRuntimeRequest, Runtime](
                  registeredRequest.request,
                  registeredRequest.id,
                  registeredRequest.startedAt,
                  s"Internal docker error",
                  Some(ex)
                )

              case e =>
                ServiceTaskFailed[SyncRuntimeRequest, Runtime](
                  registeredRequest.request,
                  registeredRequest.id,
                  registeredRequest.startedAt,
                  s"Unexpected exception",
                  Some(e)
                )
            }

            // TODO cleanup after fail?

            syncTasks.update(registeredRequest.id, newFailStatus)
            logger.warn(s"[${registeredRequest.id}] Docker pull failed: $newFailStatus")
            logger.warn(s"[${registeredRequest.id}] Docker pull logs: ${logHandler.messages.mkString("\n")}")
        }
      }
      registeredRequest
    }
  }
}