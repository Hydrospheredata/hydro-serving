package io.hydrosphere.serving.manager.service.runtime

import java.time.LocalDateTime
import java.util.UUID

import com.amazonaws.services.ecr.model.ImageNotFoundException
import com.spotify.docker.client.exceptions.DockerException
import com.spotify.docker.client.messages.ProgressMessage
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.model.{HFResult, HResult, Result}
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import io.hydrosphere.serving.manager.service.ServiceTask.ServiceTaskStatus
import io.hydrosphere.serving.manager.service._
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
  val dockerPullExecutor = ServiceTaskExecutor.withFixedPool[PullDocker, String](4)

  val syncTasks = TrieMap.empty[UUID, ServiceTask[SyncRuntimeRequest, Runtime]]

  override def lookupByModelType(modelTypes: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByModelType(
      modelTypes.map(ModelType.fromTag).toSeq
    )

  override def lookupByTag(tags: Set[String]): Future[Seq[Runtime]] =
    runtimeRepository.fetchByTags(tags.toSeq)

  override def all(): Future[Seq[Runtime]] =
    runtimeRepository.all()

  override def create(request: CreateRuntimeRequest): HFResult[ServiceTask[PullDocker, String]] = {
    logger.debug(request)
    runtimeRepository.fetchByNameAndVersion(request.name, request.version).map {
      case Some(_) =>
        logger.warn(s"Tried to add already existing runtime ${request.fullImage}")
        Result.clientError(s"Runtime ${request.fullImage} already exists")
      case None =>
        dockerPullExecutor.taskInfos.find(x => x._2.request.fullImage == request.fullImage && x._2.status == ServiceTaskStatus.Running) match {
          case Some((key, value)) =>
            Result.clientError(s"For image ${value.request.fullImage} there is already a task $key")
          case None =>
            val pullRequest = PullDocker(image = request.name, version = request.version)
            val res = dockerPullExecutor.runRequest(pullRequest)(pullDockerImage)
            res.future.flatMap { _ =>
              val rType = Runtime(
                id = 0,
                name = request.name,
                version = request.version,
                suitableModelType = request.modelTypes.map(ModelType.fromTag),
                tags = request.tags,
                configParams = request.configParams
              )
              runtimeRepository.create(rType).map { runtime =>
                logger.info(s"Runtime added: $runtime")
                Result.ok(runtime)
              }
            }
            Result.ok(res.taskStatus)
        }
    }
  }

  def pullDockerImage(request: PullDocker, updater: ServiceTaskUpdater[PullDocker, String]) = {
    try {
      updater.running()
      val logHandler = new ProgressHandler {
        override def progress(message: ProgressMessage): Unit = updater.log(message.status())
      }
      logger.info(s"Start docker pull ${request.fullImage}")
      dockerClient.pull(request.fullImage, logHandler)
      updater.finished(request.fullImage)
      Result.ok(request.fullImage)
    } catch {
      case NonFatal(err) =>
        val newFailStatus = err match {
          case ex: ImageNotFoundException =>
            updater.failed(s"Couldn't find an image ${request.fullImage}")
            ex
          case ex: DockerException =>
            updater.failed(s"Internal docker error")
            ex
          case ex: Throwable =>
            updater.failed(s"Unexpected exception")
            ex
        }
        logger.warn(s"Docker pull failed: $newFailStatus")
        throw newFailStatus
    }
  }

  override def get(id: Long): HFResult[Runtime] =
    runtimeRepository.get(id).map(_.toHResult(ClientError(s"Can't find Runtime with id $id")))

  override def getCreationStatus(requestId: UUID): HResult[ServiceTask[PullDocker, String]] = {
    val res = dockerPullExecutor.taskInfos.get(requestId).toHResult(ClientError(s"Can't find request ${requestId.toString}"))
    logger.debug(s"Status for runtime creation request $requestId = $res")
    res
  }

  def registerSync(request: SyncRuntimeRequest): HResult[ServiceTask[SyncRuntimeRequest, Runtime]] = {
    syncTasks.find(x => x._2.request.runtimeId == request.runtimeId && x._2.status == ServiceTaskStatus.Running) match {
      case Some((key, value)) =>
        Result.clientError(s"For runtime id=${value.request.runtimeId} there is already a task $key")
      case None =>
        val startedAt = LocalDateTime.now()
        val id = UUID.randomUUID()
        val status = ServiceTask.create[SyncRuntimeRequest, Runtime](id, startedAt, request)
        syncTasks += id -> status
        Result.ok(status)
    }
  }

  override def sync(request: SyncRuntimeRequest): HFResult[ServiceTask[SyncRuntimeRequest, Runtime]] = {
    logger.debug(request)
    get(request.runtimeId).map { getResult =>
      getResult.right.flatMap { runtime =>
        sync(runtime)
      }
    }
  }

  override def syncAll(): HFResult[Seq[ServiceTask[SyncRuntimeRequest, Runtime]]] = {
    runtimeRepository.all().map { runtimes =>
      Result.traverse(runtimes) { runtime =>
        sync(runtime)
      }
    }
  }

  def sync(runtime: Runtime): HResult[ServiceTask[SyncRuntimeRequest, Runtime]] = {
    ???
//    val request = SyncRuntimeRequest(runtime.id)
//    registerSync(request).right.map { registeredRequest =>
//      logger.debug(s"[${registeredRequest.id}] Registered: $registeredRequest")
//      Future {
//        val logHandler = new HistoricProgressHandler()
//        try {
//          logger.info(s"[${registeredRequest.id}] Start docker pull ${runtime.toImageDef}")
//          val request = registeredRequest.request
//          dockerClient.pull(runtime.toImageDef, logHandler)
//
//          syncTasks += registeredRequest.id ->  ServiceTask(request, registeredRequest.id, registeredRequest.startedAt, LocalDateTime.now(), runtime)
//          logger.info(s"[${registeredRequest.id}] Docker pull finished ${runtime.toImageDef}")
//        } catch {
//          case NonFatal(err) =>
//            val newFailStatus = err match {
//              case ex: ImageNotFoundException =>
//                ServiceTask[SyncRuntimeRequest, Runtime](
//                  registeredRequest.request,
//                  registeredRequest.id,
//                  registeredRequest.startedAt,
//                  s"Couldn't find an image ${runtime.toImageDef}",
//                  Some(ex)
//                )
//
//              case ex: DockerException =>
//                ServiceTask[SyncRuntimeRequest, Runtime](
//                  registeredRequest.request,
//                  registeredRequest.id,
//                  registeredRequest.startedAt,
//                  s"Internal docker error",
//                  Some(ex)
//                )
//
//              case e =>
//                ServiceTask[SyncRuntimeRequest, Runtime](
//                  registeredRequest.request,
//                  registeredRequest.id,
//                  registeredRequest.startedAt,
//                  s"Unexpected exception",
//                  Some(e)
//                )
//            }
//
//            // TODO cleanup after fail?
//
//            syncTasks.update(registeredRequest.id, newFailStatus)
//            logger.warn(s"[${registeredRequest.id}] Docker pull failed: $newFailStatus")
//            logger.warn(s"[${registeredRequest.id}] Docker pull logs: ${logHandler.messages.mkString("\n")}")
//        }
//      }
//      registeredRequest
//    }
  }
}