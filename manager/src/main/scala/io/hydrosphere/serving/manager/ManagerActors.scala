package io.hydrosphere.serving.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.manager.actor.RepositoryActor
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.concurrent.duration._
/**
  *
  */
class ManagerActors(managerServices: ManagerServices)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends Logging {

  val repoActor = system.actorOf(RepositoryActor.props(managerServices.modelManagementService))
  val indexerActors: Seq[ActorRef] = Await.result(managerServices.sourceManagementService.createWatchers(system), 5.minutes)
}
