package io.hydrosphere.serving.manager.domain.model_version

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model

case class ModelVersionView(
  id: Long,
  image: DockerImage,
  created: LocalDateTime,
  finished: Option[LocalDateTime],
  modelVersion: Long,
  modelContract: ModelContract,
  runtime: DockerImage,
  model: Model,
  hostSelector: Option[HostSelector],
  status: String,
  metadata: Map[String, String],
  applications: Seq[String]
)

object ModelVersionView {
  def fromVersion(modelVersion: ModelVersion, applications: Seq[Application]) = {
    ModelVersionView(
      id = modelVersion.id,
      image = modelVersion.image,
      created = modelVersion.created,
      finished = modelVersion.finished,
      modelVersion = modelVersion.modelVersion,
      modelContract = modelVersion.modelContract,
      runtime = modelVersion.runtime,
      model = modelVersion.model,
      hostSelector = modelVersion.hostSelector,
      status = modelVersion.status.toString,
      applications = applications.map(_.name),
      metadata = modelVersion.metadata
    )
  }
}