package io.hydrosphere.serving.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.manager.actor.RepositoryActor
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher
import io.hydrosphere.serving.manager.service.ModelManagementService
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
class ManagerActors(
  managerServices: ManagerServices
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val repoActor = system.actorOf(RepositoryActor.props(managerServices.modelManagementService))

  val indexerActors: Seq[ActorRef] = managerServices.modelSources.map {
    case (conf, modelSource) =>
      logger.info(s"SourceWatcher initialization: ${conf.name}")
      system.actorOf(SourceWatcher.props(modelSource), s"Watcher@${conf.name}")
  }.toSeq
}
