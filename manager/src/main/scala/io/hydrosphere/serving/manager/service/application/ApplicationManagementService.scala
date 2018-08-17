package io.hydrosphere.serving.manager.service.application

import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.model.api.HFResult
import spray.json.JsObject

import scala.concurrent.Future


trait ApplicationManagementService {

  def allApplications(): Future[Seq[Application]]

  def getApplication(id: Long): HFResult[Application]

  def generateInputsForApplication(appId: Long, signatureName: String): HFResult[JsObject]

  def findVersionUsage(versionId: Long): Future[Seq[Application]]

  def createApplication(
    name: String,
    namespace:Option[String],
    executionGraph: ExecutionGraphRequest,
    kafkaStreaming: Seq[ApplicationKafkaStream]
  ): HFResult[Application]

  def updateApplication(
    id: Long,
    name: String,
    namespace:Option[String],
    executionGraph: ExecutionGraphRequest,
    kafkaStreaming: Seq[ApplicationKafkaStream]
  ): HFResult[Application]

  def deleteApplication(id: Long): HFResult[Application]
}


