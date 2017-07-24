package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.model.{ModelRuntime, ModelService, ModelServiceInstance}

/**
  *
  */
trait RuntimeDeployService {
  def deploy(runtime: ModelService): String

  def serviceList(): Seq[Long]

  def deleteService(serviceId: Long)

  def serviceInstances(): Seq[ModelServiceInstance]

  def serviceInstances(serviceId: Long): Seq[ModelServiceInstance]
}
