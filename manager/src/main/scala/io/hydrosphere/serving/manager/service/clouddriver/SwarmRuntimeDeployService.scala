package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.model.ModelRuntime

import scala.concurrent.Future

/**
  *
  */
class SwarmRuntimeDeployService extends RuntimeDeployService {
  override def scale(runtimeName: String, scale: Int): Future[Unit] = ???

  override def deploy(runtime: ModelRuntime): String = ???

  override def getRuntime(runtimeName: String): ModelRuntime = ???

  override def runtimeList(): Seq[ModelRuntime] = ???

  override def deleteRuntime(runtimeName: String): Unit = ???
}
