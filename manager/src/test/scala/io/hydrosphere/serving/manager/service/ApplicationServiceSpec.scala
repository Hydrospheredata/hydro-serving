package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType.Tensorflow
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.service.application.ApplicationManagementServiceImpl

import scala.concurrent.{Await, ExecutionContext, Future}

class ApplicationServiceSpec extends GenericUnitTest {

  implicit val ctx = ExecutionContext.global

  "Applications" should "be enriched" in {

    val modelsMap = Future.successful(models())
    val runtimeMap = Future.successful(runtime())
    val apps = Seq(app())

    import scala.concurrent.duration._

    val result = Await.result(appService.enrichServiceKeyDescription(apps, runtimeMap, modelsMap), 1 second)
    val serviceDescription = result.head.executionGraph.stages.head.services.head.serviceDescription
    assert(serviceDescription.runtimeName.contains("runtime:latest"))
    assert(serviceDescription.modelName.contains("model_name:1"))
  }

  def models() = Map(1l -> ModelVersion(
    id = 1,
    imageName = "",
    imageTag = "",
    imageSHA256 = "",
    created = LocalDateTime.now(),
    modelName = "model_name",
    modelVersion = 1,
    modelType = Tensorflow("1.1.0"),
    model = None,
    modelContract = ModelContract()
  ))

  def runtime() =
    Map(1l -> Runtime(
      id = 1,
      name = "runtime",
      version = "latest",
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
