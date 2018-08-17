package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.grpc.applications.{ExecutionGraph, ExecutionService, ExecutionStage, KafkaStreaming, Application => GApp}
import io.hydrosphere.serving.manager.grpc.entities.{Environment=>GEnvironment, Runtime=>GRuntime, ModelVersion=>GModelVersion, Model=>GModel}

case class Application(
  id: Long,
  name: String,
  namespace: Option[String],
  contract: ModelContract,
  executionGraph: ApplicationExecutionGraph,
  kafkaStreaming: List[ApplicationKafkaStream]
)

object Application {
  def toGrpc(app: Application): GApp = {
    GApp(
      id = app.id,
      name = app.name,
      contract = Option(app.contract),
      namespace = app.namespace.getOrElse(""),
      kafkaStreaming = app.kafkaStreaming.map { k =>
        KafkaStreaming(
          consumerId = k.consumerId.getOrElse(s"appConsumer${app.id}"),
          sourceTopic = k.sourceTopic,
          destinationTopic = k.destinationTopic,
          errorTopic = k.errorTopic.getOrElse("")
        )
      },
      executionGraph = Option(ExecutionGraph(
        app.executionGraph.stages.zipWithIndex.map {
          case (stage, idx) => ExecutionStage(
            stageId = ApplicationStage.stageId(app.id, idx),
            signature = stage.signature,
            dataTypes=stage.dataProfileFields,
            services = stage.services.map(service=>{
              ExecutionService(
                weight = service.weight,
                environment = Some(GEnvironment(
                  id=service.environment.id,
                  name=service.environment.name
                )),
                runtime = Some(GRuntime(
                  id=service.runtime.id,
                  name=service.runtime.name,
                  version = service.runtime.version
                )),
                modelVersion = Some(GModelVersion(
                  id=service.modelVersion.id,
                  imageName = service.modelVersion.imageName,
                  imageTag = service.modelVersion.imageTag,
                  contract = Some(service.modelVersion.modelContract),
                  dataTypes = service.modelVersion.dataProfileTypes.getOrElse(Map()),
                  model = service.modelVersion.model.map(m=>{
                    GModel(
                      id=m.id,
                      name=m.name
                    )
                  })
                ))
              )
            })
          )
        }
      ))
    )
  }
}