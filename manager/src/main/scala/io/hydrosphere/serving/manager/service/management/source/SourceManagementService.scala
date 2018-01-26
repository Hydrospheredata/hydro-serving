package io.hydrosphere.serving.manager.service.management.source

import java.nio.file.Path

import akka.actor.ActorRef
import io.hydrosphere.serving.manager.model.ModelSourceConfigAux
import io.hydrosphere.serving.manager.service.modelsource.ModelSource

import scala.concurrent.Future



trait SourceManagementService {
  def addSource(createModelSourceRequest: CreateModelSourceRequest): Future[ModelSourceConfigAux]

  def addSource(modelSourceConfigAux: ModelSourceConfigAux): Future[ActorRef]

  def createWatcher(modelSource: ModelSource): Future[ActorRef]

  def getSources: Future[List[ModelSource]]

  def getSourceConfigs: Future[List[ModelSourceConfigAux]]

  def getLocalPath(url: String): Future[Path]

  def createWatchers: Future[Seq[ActorRef]]
}
