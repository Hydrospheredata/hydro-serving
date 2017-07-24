package io.hydrosphere.serving.manager.service.clouddriver

import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.{AdvertisedConfiguration, SwarmCloudDriverConfiguration}
import io.hydrosphere.serving.manager.model.{ModelRuntime, ModelService, ModelServiceInstance}
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
class SwarmRuntimeDeployService(
  dockerClient: DockerClient,
  swarmCloudDriverConfiguration: SwarmCloudDriverConfiguration,
  advertisedConfiguration: AdvertisedConfiguration
) extends RuntimeDeployService with Logging {
  
  override def deploy(runtime: ModelService): String = ???

  override def serviceList(): Seq[Long] = ???

  override def deleteService(serviceId: Long): Unit = ???

  override def serviceInstances(): Seq[ModelServiceInstance] = ???

  override def serviceInstances(serviceId: Long): Seq[ModelServiceInstance] = ???
}
