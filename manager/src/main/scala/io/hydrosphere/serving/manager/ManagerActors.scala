package io.hydrosphere.serving.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.manager.actor.RepositoryActor
import io.hydrosphere.serving.manager.service.clouddriver.CachedProxyRuntimeDeployService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  *
  */
class ManagerActors(
  managerServices: ManagerServices
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends Logging {

  implicit private val ex: ExecutionContext = system.dispatcher

  val repoActor: ActorRef = system.actorOf(RepositoryActor.props(managerServices.modelManagementService))

  val indexerActors: Seq[ActorRef] = managerServices.sourceManagementService.createWatchers(system)

  //create runtimeDeployService cache refresh action
  //TODO change to event subscription
  managerServices.runtimeDeployService match {
    case c: CachedProxyRuntimeDeployService =>
      system.scheduler.schedule(0 seconds, 5 seconds)(c.refreshCache())
    case _ => logger.info(s"Cache disabled for RuntimeDeployService")
  }

}
