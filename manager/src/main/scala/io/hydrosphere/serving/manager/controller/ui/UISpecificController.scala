package io.hydrosphere.serving.manager.controller.ui

import io.hydrosphere.serving.manager.model.{Model, ModelBuild, ModelRuntime}


case class ModelInfo(
  mode:Model,
  lastModelBuild:ModelBuild,
  lastModelRuntime:ModelRuntime
)

class UISpecificController extends UIJsonSupport{

}
