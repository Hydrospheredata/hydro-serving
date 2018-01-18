package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.connector._
import io.hydrosphere.serving.manager.model.api.{ContractOps, DataGenerator, SignatureChecker}
import io.hydrosphere.serving.manager.model.{Application, ApplicationExecutionGraph, ModelService}
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, ModelServiceRepository}

import scala.concurrent.Await
import org.apache.logging.log4j.scala.Logging
import spray.json.JsObject

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
case class ApplicationCreateOrUpdateRequest(
  id: Option[Long],
  serviceName: String,
  executionGraph: ApplicationExecutionGraph,
  sourcesList: Option[List[Long]]
) {

  def toApplication: Application = {
    Application(
      id = this.id.getOrElse(0),
      name = this.serviceName,
      executionGraph = this.executionGraph,
      sourcesList = this.sourcesList.getOrElse(List())
    )
  }
}

case class ServiceWithSignature(s: ModelService, signatureName: String)

trait ServingManagementService {

  def allApplications(): Future[Seq[Application]]

  def applicationsByModelServiceIds(servicesIds: Seq[Long]): Future[Seq[Application]]

  def createApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]

  def updateApplications(req: ApplicationCreateOrUpdateRequest): Future[Application]

  def deleteApplication(id: Long): Future[Unit]

  def getApplication(id: Long): Future[Option[Application]]

  def serve(req: ServeRequest): Future[ExecutionResult]

  def generateModelPayload(modelName: String, modelVersion: Long, signature: String): Future[Seq[JsObject]]

  def generateModelPayload(modelName: String, signature: String): Future[Seq[JsObject]]

  def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean]

  def generateInputsForApplication(appId: Long): Future[Option[Seq[JsObject]]]
}

class ServingManagementServiceImpl(
  modelServiceRepository: ModelServiceRepository,
  runtimeMeshConnector: RuntimeMeshConnector,
  applicationRepository: ApplicationRepository,
  runtimeManagementService: DDRuntimeManagementService
)(implicit val ex: ExecutionContext) extends ServingManagementService with Logging {

  override def serve(req: ServeRequest): Future[ExecutionResult] = {
    import ToPipelineStages._

    def buildStages[A](target: Option[A], error: => String)
      (implicit conv: ToPipelineStages[A]): Future[Seq[ExecutionUnit]] = {

      target match {
        case None => Future.failed(new IllegalArgumentException(error))
        case Some(a) => Future.successful(conv.toStages(a, req.servePath))
      }
    }

    val stagesFuture = req.serviceKey match {
      case ApplicationKey(id) =>
        val f = getApplication(id)
        f.flatMap(w => buildStages(w, s"Can't find Application with id: $id"))
      case ApplicationName(name) =>
        val f = applicationRepository.getByName(name)
        f.flatMap(w => buildStages(w, s"Can't find Application with name: $name"))
      case ModelById(id) =>
        val f = modelServiceRepository.get(id)
        f.flatMap(m => buildStages(m, s"Can't find model with id: $id"))
      case ModelByName(name, None) =>
        val f = modelServiceRepository.getLastModelServiceByModelName(name)
        f.flatMap(m => buildStages(m, s"Can find model with name: $name"))
      case ModelByName(name, Some(version)) =>
        val f = modelServiceRepository.getLastModelServiceByModelNameAndVersion(name, version)
        f.flatMap(m => buildStages(m, s"Can find model with name: $name, version: $version"))
    }

    stagesFuture.flatMap(stages => {
      val cmd = ExecutionCommand(
        headers = req.headers,
        json = req.inputData,
        pipe = stages
      )
      logger.info(s"TRY INVOKE $cmd")
      runtimeMeshConnector.execute(cmd)
    })
  }

  override def allApplications(): Future[Seq[Application]] =
    applicationRepository.all()

  override def applicationsByModelServiceIds(servicesIds: Seq[Long]): Future[Seq[Application]] =
    applicationRepository.byModelServiceIds(servicesIds)

  override def createApplications(req: ApplicationCreateOrUpdateRequest): Future[Application] =
    applicationRepository.create(req.toApplication)

  override def updateApplications(req: ApplicationCreateOrUpdateRequest): Future[Application] =
    fetchAndValidate(req.executionGraph).flatMap(_ => {
      val service = req.toApplication
      applicationRepository.update(service).map(_ => service)
    })


  override def deleteApplication(id: Long): Future[Unit] =
    applicationRepository.get(id).flatMap({
      case Some(x) =>
        if (x.sourcesList.nonEmpty) {
          Future.traverse(x.sourcesList)(s => runtimeManagementService.deleteService(s))
            .flatMap(_ => applicationRepository.delete(id).map(_ => Unit))
        } else {
          applicationRepository.delete(id).map(_ => Unit)
        }
      case _ => Future.successful(())
    })

  private def fetchAndValidate(req: ApplicationExecutionGraph): Future[Unit] = {
    val servicesIds = req.stages.flatMap(s => s.services.map(c => c.serviceId))
    modelServiceRepository.fetchByIds(servicesIds)
      .map(s =>
        if (s.length != servicesIds.length) {
          throw new IllegalArgumentException("Can't find all services")
        })
  }

  override def generateModelPayload(modelName: String, modelVersion: Long, signature: String): Future[Seq[JsObject]] = {
    modelServiceRepository.getLastModelServiceByModelNameAndVersion(modelName, modelVersion).map {
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) =>
        val res = DataGenerator.forContract(service.modelRuntime.modelContract, signature).get.generateInputs
        Seq(ContractOps.TensorProtoOps.jsonify(res))
    }
  }

  override def generateModelPayload(modelName: String, signature: String): Future[Seq[JsObject]] = {
    modelServiceRepository.getLastModelServiceByModelName(modelName).map {
      case None => throw new IllegalArgumentException(s"Can't find service for modelName=$modelName")
      case Some(service) =>
        val res = DataGenerator.forContract(service.modelRuntime.modelContract, signature).get.generateInputs
        Seq(ContractOps.TensorProtoOps.jsonify(res))
    }
  }

  override def getApplication(id: Long): Future[Option[Application]] =
    applicationRepository.get(id)

  override def checkApplicationSchema(req: ApplicationCreateOrUpdateRequest): Future[Boolean] = {
    val stages = req.executionGraph.stages.zipWithIndex.map {
      case (stage, stIdx) =>
        val servicesId = stage.services.map(s => s.serviceId -> s.signatureName).toMap
        val services = servicesId.map {
          case (id, sig) =>
            val modelService = Await.result(modelServiceRepository.get(id), 1.minute)
              .getOrElse(throw new IllegalArgumentException(s"Service $id is not found."))
            modelService -> sig
        }.map { case (s, sig) => ServiceWithSignature(s, sig) }.toSeq
        createStageSignature(stIdx, services)
    }

    Future(
      stages.zip(stages.tail).forall {
        case (sig1, sig2) => SignatureChecker.areSequentiallyCompatible(sig1, sig2)
      }
    )
  }

  private def createStageSignature(idx: Long, stages: Seq[ServiceWithSignature]): ModelSignature = {
    val signatures = stages.map { info =>
      info.s.modelRuntime.modelContract.signatures
        .find(_.signatureName == info.signatureName)
        .getOrElse(throw new IllegalArgumentException(s"${info.signatureName} signature doesn't exist"))
    }
    signatures.fold(ModelSignature.defaultInstance)(ContractOps.ModelSignatureOps.merge)
  }

  override def generateInputsForApplication(appId: Long): Future[Option[Seq[JsObject]]] = {
    applicationRepository.get(appId).flatMap {
      case Some(app) =>
        inferAppInputSchema(app).map { schema =>
          val data = DataGenerator(schema).generateInputs
          Some(Seq(ContractOps.TensorProtoOps.jsonify(data)))
        }
      case None =>
        Future.successful(None)
    }
  }

  private def inferAppInputSchema(application: Application): Future[ModelSignature] = {
    val stages = application.executionGraph.stages.map(_.services.map(s => s.serviceId -> s.signatureName).toMap)
    val headServices = Future.sequence {
      stages.head.map {
        case (sId, signature) =>
          modelServiceRepository.get(sId).map { maybeService =>
            val service = maybeService
              .getOrElse(throw new IllegalArgumentException(s"Service with id $sId is not found"))
            service.modelRuntime.modelContract.signatures
              .find(_.signatureName == signature)
              .getOrElse(throw new IllegalArgumentException(s"Signature $signature for service $sId is not found"))
          }
      }.toList
    }
    headServices.map {
      _.foldRight(ModelSignature()) {
        case (sig1, sig2) => ContractOps.ModelSignatureOps.merge(sig1, sig2)
      }
    }
  }
}
