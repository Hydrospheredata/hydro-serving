package io.hydrosphere.serving.manager.api.http.controller.model

import java.nio.file.Path

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.model.api.{HResult, Result}

import scala.reflect.ClassTag

case class ModelUpload(
  tarballPath: Path,
  name: Option[String] = None,
  modelType: Option[String] = None,
  runtimeName: String,
  runtimeVersion: String,
  hostSelectorName: Option[String] = None,
  contract: Option[ModelContract] = None,
  description: Option[String] = None
)
