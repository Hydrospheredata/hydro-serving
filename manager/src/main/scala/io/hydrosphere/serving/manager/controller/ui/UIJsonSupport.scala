package io.hydrosphere.serving.manager.controller.ui

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.service._

/**
  *
  */
trait UIJsonSupport extends ManagerJsonSupport {
  implicit val modelInfoFormat = jsonFormat6(ModelInfo)

  implicit val kafkaStreamingParamsFormat = jsonFormat4(KafkaStreamingParams)
  implicit val serviceWeightDetailsFormat = jsonFormat2(ServiceWeightDetails)
  implicit val WeightedServiceDetailsFormat = jsonFormat4(WeightedServiceDetails)

  implicit val uiWeightedServiceCreateOrUpdateRequestFormat = jsonFormat4(UIWeightedServiceCreateOrUpdateRequest)

  implicit val serviceInfoFormat=jsonFormat2(UIServiceInfo)
  implicit val runtimeInfoFormat=jsonFormat2(UIRuntimeInfo)

}
