package io.hydrosphere.serving.manager.infrastructure.envoy

import io.hydrosphere.serving.manager.domain.application.{Application, ApplicationStage, DetailedServiceDescription}
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionService, ExecutionStage, KafkaStreaming, Application => GApp}
import io.hydrosphere.serving.manager.grpc.entities

object Converters {

  def grpcApp(app: Application): GApp = {
    GApp(
      id = app.id,
      name = app.name,
      contract = Option(app.contract),
      namespace = app.namespace.getOrElse(""),
      executionGraph = Option(ExecutionGraph(
        app.executionGraph.stages.zipWithIndex.map {
          case (stage, idx) => ExecutionStage(
            stageId = ApplicationStage.stageId(app.id, idx),
            signature = stage.signature,
            services = stage.services.map(grpcService)
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
      imageName = modelVersion.image.name,
      imageTag = modelVersion.image.tag,
      model = Some(grpcModel(modelVersion.model)),
      contract = Some(modelVersion.modelContract),
    )
  }

  def grpcHostSelector(hostSelector: HostSelector): entities.Environment = {
    entities.Environment(
      id = hostSelector.id,
      name = hostSelector.name
    )
  }

  def grpcRuntime(modelVersion: ModelVersion): entities.Runtime = {
    entities.Runtime(
      id = 0,
      name = modelVersion.runtime.name,
      version = modelVersion.runtime.tag
    )
  }

  def grpcService(desc: DetailedServiceDescription): ExecutionService = {
    ExecutionService(
      runtime = Some(grpcRuntime(desc.modelVersion)),
      modelVersion = Some(grpcModelVersion(desc.modelVersion)),
      environment = desc.modelVersion.hostSelector.map(grpcHostSelector),
      weight = desc.weight
    )
  }

}
