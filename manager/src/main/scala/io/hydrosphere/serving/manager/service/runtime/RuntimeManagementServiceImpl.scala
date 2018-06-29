package io.hydrosphere.serving.manager.service.runtime

import java.util.UUID
import java.util.concurrent.Executors

import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.model.Result.ClientError
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{CreateRuntimeRequest, PullRuntime, Runtime}
import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.repository.{RuntimePullRepository, RuntimeRepository}
import io.hydrosphere.serving.manager.util.task.ServiceTask
import org.apache.logging.log4j.scala.Logging

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

class RuntimeManagementServiceImpl(
  runtimeRepository: RuntimeRepository,
  runtimePullRepository: RuntimePullRepository,
  dockerClient: DockerClient
)(
  implicit ex: ExecutionContext
) extends RuntimeManagementService with Logging {
  val dockerPullExecutor = new RuntimePullExecutor(
    dockerClient,
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  )

  val syncTasks = TrieMap.empty[UUID, ServiceTask[SyncRuntimeRequest, Runtime]]

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
          case Some(running) =>
            Result.clientErrorF(s"Image ${request.fullImage} is already creating")
          case None =>
            val task = dockerPullExecutor.execute(request)
            task.future.foreach { runtime =>
              runtimeRepository.create(runtime).map { runtime =>
                logger.info(s"Runtime added: $runtime")
                Result.ok(runtime)
              }
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

//  def registerSync(request: SyncRuntimeRequest): HResult[ServiceTask[SyncRuntimeRequest, Runtime]] = {
//    syncTasks.find(x => x._2.request.runtimeId == request.runtimeId && x._2.status == ServiceTaskStatus.Running) match {
//      case Some((key, value)) =>
//        Result.clientError(s"For runtime id=${value.request.runtimeId} there is already a task $key")
//      case None =>
//        val startedAt = LocalDateTime.now()
//        val id = UUID.randomUUID()
//        val status = ServiceTask.create[SyncRuntimeRequest, Runtime](id, startedAt, request)
//        syncTasks += id -> status
//        Result.ok(status)
//    }
//  }

//  override def sync(request: SyncRuntimeRequest): HFResult[ServiceTask[SyncRuntimeRequest, Runtime]] = {
//    logger.debug(request)
//    get(request.runtimeId).map { getResult =>
//      getResult.right.flatMap { runtime =>
//        sync(runtime)
//      }
//    }
//  }
//
//
//  override def syncAll(): HFResult[Seq[ServiceTask[SyncRuntimeRequest, Runtime]]] = {
//    runtimeRepository.all().map { runtimes =>
//      Result.traverse(runtimes) { runtime =>
//        sync(runtime)
//      }
//    }
//  }
//
//  def sync(runtime: Runtime): HResult[ServiceTask[SyncRuntimeRequest, Runtime]] = {
//    ???
////    val request = SyncRuntimeRequest(runtime.id)
////    registerSync(request).right.map { registeredRequest =>
////      logger.debug(s"[${registeredRequest.id}] Registered: $registeredRequest")
////      Future {
////        val logHandler = new HistoricProgressHandler()
////        try {
////          logger.info(s"[${registeredRequest.id}] Start docker pull ${runtime.toImageDef}")
////          val request = registeredRequest.request
////          dockerClient.pull(runtime.toImageDef, logHandler)
////
////          syncTasks += registeredRequest.id ->  ServiceTask(request, registeredRequest.id, registeredRequest.startedAt, LocalDateTime.now(), runtime)
////          logger.info(s"[${registeredRequest.id}] Docker pull finished ${runtime.toImageDef}")
////        } catch {
////          case NonFatal(err) =>
////            val newFailStatus = err match {
////              case ex: ImageNotFoundException =>
////                ServiceTask[SyncRuntimeRequest, Runtime](
////                  registeredRequest.request,
////                  registeredRequest.id,
////                  registeredRequest.startedAt,
////                  s"Couldn't find an image ${runtime.toImageDef}",
////                  Some(ex)
////                )
////
////              case ex: DockerException =>
////                ServiceTask[SyncRuntimeRequest, Runtime](
////                  registeredRequest.request,
////                  registeredRequest.id,
////                  registeredRequest.startedAt,
////                  s"Internal docker error",
////                  Some(ex)
////                )
////
////              case e =>
////                ServiceTask[SyncRuntimeRequest, Runtime](
////                  registeredRequest.request,
////                  registeredRequest.id,
////                  registeredRequest.startedAt,
////                  s"Unexpected exception",
////                  Some(e)
////                )
////            }
////
////            // TODO cleanup after fail?
////
////            syncTasks.update(registeredRequest.id, newFailStatus)
////            logger.warn(s"[${registeredRequest.id}] Docker pull failed: $newFailStatus")
////            logger.warn(s"[${registeredRequest.id}] Docker pull logs: ${logHandler.messages.mkString("\n")}")
////        }
////      }
////      registeredRequest
////    }
//  }
}