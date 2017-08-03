package io.hydrosphere.serving.manager.controller.ui

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport

/**
  *
  */
trait UIJsonSupport extends ManagerJsonSupport{
  implicit val modelInfoFormat = jsonFormat3(ModelInfo)

}
