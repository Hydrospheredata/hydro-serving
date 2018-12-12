package io.hydrosphere.serving.manager.domain.clouddriver

import io.hydrosphere.serving.manager.domain.image.DockerImage

case class CloudService(
  id: Long,
  serviceName: String,
  statusText: String,
  cloudDriverId: String,
  image: DockerImage,
  instances: Seq[ServiceInstance]
)
