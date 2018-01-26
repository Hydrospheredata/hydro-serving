package io.hydrosphere.serving.manager.service.management.application

import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}

import scala.concurrent.Future

trait ApplicationManagementService {
  def serveGrpcApplication(data: PredictRequest): Future[PredictResponse]

  /*def allApplications(): Future[Seq[Application]]

  def applicationsByModelServiceIds(servicesIds: Seq[Long]): Future[Seq[Application]]

  def createApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]

  def updateApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]

  def deleteApplication(id: Long): Future[Unit]

  def getApplication(id: Long): Future[Option[Application]]

  def serve(req: ServeRequest): Future[ExecutionResult]

  def generateModelPayload(modelName: String, modelVersion: Long, signature: String): Future[Seq[JsObject]]

  def generateModelPayload(modelName: String, signature: String): Future[Seq[JsObject]]

  def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean]

  def generateInputsForApplication(appId: Long): Future[Option[Seq[JsObject]]]*/
}
