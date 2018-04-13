package io.hydrosphere.serving.manager.service.build_script

import io.hydrosphere.serving.manager.model.db.Model

import scala.concurrent.Future

trait BuildScriptManagementService {
  def fetchScriptForModel(model: Model): Future[String]
}
