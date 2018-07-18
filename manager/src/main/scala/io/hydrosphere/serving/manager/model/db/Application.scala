package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.grpc.applications.{Application => GApp, ExecutionGraph, ExecutionStage, KafkaStreaming}

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
      executionGraph = Option(ExecutionGraph(
        app.executionGraph.stages.zipWithIndex.map {
          case (stage, idx) => ExecutionStage(
            stageId = ApplicationStage.stageId(app.id, idx),
            signature = stage.signature
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
}