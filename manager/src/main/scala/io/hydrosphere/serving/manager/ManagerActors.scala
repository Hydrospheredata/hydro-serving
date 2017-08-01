package io.hydrosphere.serving.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.manager.actor.IndexerActor
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
class ManagerActors(managerServices: ManagerServices)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends Logging {


  val indexerActors: Seq[ActorRef] = managerServices.modelSources.map {
    case (conf: ModelSourceConfiguration, modelSource: ModelSource) =>
      logger.info(s"ModeSource IndexerActor initialization: ${conf.name}")
      system.actorOf(IndexerActor.props(modelSource, conf, managerServices.modelManagementService), s"Indexer@${conf.name}")
  }.toSeq
}
