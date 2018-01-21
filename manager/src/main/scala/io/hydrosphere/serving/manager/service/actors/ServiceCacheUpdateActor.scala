package io.hydrosphere.serving.manager.service.actors

import akka.actor.Props
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.clouddriver.CachedProxyRuntimeDeployService

import scala.concurrent.duration._

class ServiceCacheUpdateActor(val cachedProxyRuntimeDeployService: CachedProxyRuntimeDeployService)
  extends SelfScheduledActor(0.seconds, 5.seconds)(Timeout(30.seconds)) {

  override def onTick(): Unit = {
    //TODO
    //cachedProxyRuntimeDeployService.refreshCache()
  }

  override def recieveNonTick: Receive = {
    case _ =>
  }
}

object ServiceCacheUpdateActor {
  def props(cachedProxyRuntimeDeployService: CachedProxyRuntimeDeployService) = Props(new ServiceCacheUpdateActor(cachedProxyRuntimeDeployService))
}


