package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.model.ModelServiceInstance
import io.hydrosphere.serving.model.ModelService
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
class CachedProxyRuntimeDeployService(original: RuntimeDeployService) extends RuntimeDeployService with Logging {
  private var serviceCache: Map[Long, ServiceInfo] = Map()

  private var instancesCache: Map[Long, Seq[ModelServiceInstance]] = Map()

  def refreshCache(): Unit = {
    val servicaMap = original.serviceList().map(s => s.id -> s).toMap

    val instancesMap = scala.collection.mutable.Map[Long, Seq[ModelServiceInstance]]()

    servicaMap.values.foreach(s => {
      val instances = original.serviceInstances(s.id)
      if (instances.nonEmpty) {
        instancesMap.put(s.id, instances)
      }
    })

    serviceCache = servicaMap
    instancesCache = instancesMap.toMap
  }

  override def serviceInstances(serviceId: Long): Seq[ModelServiceInstance] =
    instancesCache.get(serviceId) match {
      case Some(x) => x
      case _ => Seq()
    }

  override def serviceList(): Seq[ServiceInfo] =
    serviceCache.values.toSeq

  override def service(serviceId: Long): Option[ServiceInfo] =
    serviceCache.get(serviceId)

  override def deploy(runtime: ModelService): String =
    original.deploy(runtime)

  override def deleteService(serviceId: Long): Unit =
    original.deleteService(serviceId)
}
