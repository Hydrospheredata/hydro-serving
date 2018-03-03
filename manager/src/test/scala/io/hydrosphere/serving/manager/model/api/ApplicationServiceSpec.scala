package io.hydrosphere.serving.manager.model.api

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType.Tensorflow
import io.hydrosphere.serving.manager.service.ApplicationManagementServiceImpl
import org.scalatest.FlatSpec


import scala.concurrent.{Await, ExecutionContext, Future}

class ApplicationServiceSpec extends FlatSpec {

  implicit val ctx = ExecutionContext.global

  "Applications" should "be enriched" in {

    val modelsMap = Future.successful(models())
    val runtimeMap = Future.successful(runtime())
    val apps = Future.successful(Seq(app()))

    import scala.concurrent.duration._

    val result = Await.result(appService.enrichServiceKeyDescription(apps, runtimeMap, modelsMap), 1 second)
    val serviceDescription = result.head.executionGraph.stages.head.services.head.serviceDescription
    assert(serviceDescription.runtimeName == Some("runtime"))
    assert(serviceDescription.modelName == Some("model_name"))

  }

  def models() = Map(1l -> ModelVersion(
    id = 1,
    imageName = "",
    imageTag = "",
    imageSHA256 = "",
    created = LocalDateTime.now(),
    modelName = "model_name",
    modelVersion = 1,
    modelType = Tensorflow(),
    source = None,
    model = None,
    modelContract = ModelContract()
  ))

  def runtime() =
    Map(1l -> Runtime(
      id = 1,
      name = "runtime",
      version = "",
      suitableModelType = List(),
      tags = List(),
      configParams = Map()
    ))


  def app()  = Application(
    id = 1,
    name = "app",
    contract = ModelContract(),
    kafkaStreaming = List(),
    executionGraph = ApplicationExecutionGraph(
      stages = List(
        ApplicationStage(
          signature = None,
          services = List(
            WeightedService(
              weight = 100,
              signature = None,
              serviceDescription = ServiceKeyDescription(
                1, Some(1), None
              )
            )
          )
        )
      )
    )
  )


  val appService = new ApplicationManagementServiceImpl(
    null, null, null, null, null, null, null
  )

}
