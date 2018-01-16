package io.hydrosphere.serving.manager.controller.ui

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.ui._

/**
  *
  */
trait UIJsonSupport extends ManagerJsonSupport {
  implicit val modelInfoFormat = jsonFormat5(ModelInfo)

  implicit val kafkaStreamingParamsFormat = jsonFormat4(KafkaStreamingParams)
  implicit val serviceWeightDetailsFormat = jsonFormat2(ServiceWeightDetails)
  implicit val applicationDetailsFormat = jsonFormat4(ApplicationDetails)

  implicit val uiApplicationFormat = jsonFormat3(UIServiceWeight)
  implicit val uiApplicationCreateOrUpdateRequestFormat = jsonFormat4(UIApplicationCreateOrUpdateRequest)

  implicit val serviceInfoFormat=jsonFormat2(UIServiceInfo)
  implicit val runtimeInfoFormat=jsonFormat2(UIRuntimeInfo)

}
