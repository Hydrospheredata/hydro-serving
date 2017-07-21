package io.hydrosphere.serving.manager.service.clouddriver

import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.{AdvertisedConfiguration, SwarmCloudDriverConfiguration}
import io.hydrosphere.serving.manager.model.{ModelRuntime, ModelService}
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
class SwarmRuntimeDeployService(
  dockerClient: DockerClient,
  swarmCloudDriverConfiguration: SwarmCloudDriverConfiguration,
  advertisedConfiguration: AdvertisedConfiguration
) extends RuntimeDeployService with Logging {

  //override def scale(runtimeName: String, scale: Int): Unit = ???

  override def deploy(service: ModelService): String = ???

  override def getRuntime(runtimeName: String): ModelRuntime = ???

  override def runtimeList(): Seq[ModelRuntime] = ???

  override def deleteRuntime(runtimeName: String): Unit = ???
}
