package io.hydrosphere.serving.manager.service.clouddriver

import io.hydrosphere.serving.manager.model.ModelRuntime

import scala.concurrent.Future

/**
  *
  */
trait RuntimeDeployService {
  def scale(runtimeName: String, scale: Int): Future[Unit]

  def deploy(runtime:ModelRuntime):String

  def getRuntime(runtimeName:String):ModelRuntime

  def runtimeList():Seq[ModelRuntime]

  def deleteRuntime(runtimeName:String)

  /*


List<RuntimeInstance> runtimeInstances();

List<RuntimeInstance> runtimeInstances(String runtimeName);
*/
}
