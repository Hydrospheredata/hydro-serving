package io.hydrosphere.serving.manager.infrastructure.clouddriver.docker

import java.util

import com.spotify.docker.client.messages.PortBinding
import io.hydrosphere.serving.manager.domain.clouddriver.DefaultConstants.{DEFAULT_HTTP_PORT, fakeHttpServices, specialNamesByIds}
import io.hydrosphere.serving.manager.domain.clouddriver._

object DockerUtil {
  def createPortBindingsMap(): util.Map[String, util.List[PortBinding]] = {
    val publishPorts = new util.HashMap[String, util.List[PortBinding]]()
    val bindingsList = new util.ArrayList[PortBinding]()
    bindingsList.add(PortBinding.randomPort("0.0.0.0"))
    publishPorts.put(DefaultConstants.DEFAULT_APP_PORT.toString, bindingsList)
    publishPorts
  }

  def createFakeHttpServices(services: Seq[CloudService]): Seq[CloudService] =
    services.filter(cs => fakeHttpServices.contains(cs.id))
      .map { cs =>
        val fakeId = fakeHttpServices(cs.id)
        val fakeName = specialNamesByIds(fakeId)

        cs.copy(
          id = fakeId,
          serviceName = fakeName,
          instances = cs.instances.map(s => s.copy(
            advertisedPort = DEFAULT_HTTP_PORT,
            mainApplication = s.mainApplication.copy(port = DEFAULT_HTTP_PORT)
          ))
        )
      }
}