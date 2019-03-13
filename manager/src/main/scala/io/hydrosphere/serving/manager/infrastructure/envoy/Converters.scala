package io.hydrosphere.serving.manager.infrastructure.envoy

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.application.{Application, PipelineStage, ModelVariant}
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionService, ExecutionStage, KafkaStreaming, Application => GApp}
import io.hydrosphere.serving.manager.grpc.entities
import io.hydrosphere.serving.manager.grpc.entities.DockerImage

object Converters {

  def grpcApp(app: Application): GApp = {
    GApp(
      id = app.id,
      name = app.name,
      contract = Option(ModelContract("", Some(app.signature))),
      namespace = app.namespace.getOrElse(""),
      executionGraph = Option(ExecutionGraph(
        app.executionGraph.stages.zipWithIndex.map {
          case (stage, idx) => ExecutionStage(
            stageId = PipelineStage.stageId(app.id, idx),
            signature = Some(stage.signature),
            services = stage.modelVariants.map(grpcService)
          )
        }
      )),
      kafkaStreaming = app.kafkaStreaming.map { k =>
        KafkaStreaming(
          consumerId = k.consumerId.getOrElse(s"appConsumer${app.id}"),
          sourceTopic = k.sourceTopic,
          destinationTopic = k.destinationTopic,
          errorTopic = k.errorTopic.getOrElse("")
        )
      }
    )
  }

  def grpcModel(model: Model): entities.Model = {
    entities.Model(
      id = model.id,
      name = model.name
    )
  }

  def grpcModelVersion(modelVersion: ModelVersion): entities.ModelVersion = {
    entities.ModelVersion(
      id = modelVersion.id,
      version = modelVersion.modelVersion,
      modelType = "",
      status = modelVersion.status.toString,
      selector = modelVersion.hostSelector.map(grpcHostSelector),
      image = Some(DockerImage(modelVersion.image.name, modelVersion.image.tag)),
      imageSha = modelVersion.image.sha256.getOrElse(""),
      model = Some(grpcModel(modelVersion.model)),
      contract = Some(modelVersion.modelContract),
      runtime = Some(DockerImage(modelVersion.runtime.name, modelVersion.runtime.tag))
    )
  }

  def grpcHostSelector(hostSelector: HostSelector): entities.HostSelector = {
    entities.HostSelector(
      id = hostSelector.id,
      name = hostSelector.name
    )
  }

  def grpcService(desc: ModelVariant): ExecutionService = {
    ExecutionService(
      modelVersion = Some(grpcModelVersion(desc.modelVersion)),
      weight = desc.weight
    )
  }

}
