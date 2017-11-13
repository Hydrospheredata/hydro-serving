package io.hydrosphere.serving.streaming.service

import java.time.LocalDateTime
import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.connector._
import io.hydrosphere.serving.model.Application
import io.hydrosphere.serving.streaming._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Serve batch of json object
  * Returns json messages
  */
trait ServingProcessor {

  def serve(input: Seq[String]): Future[Seq[String]]

}

/**
  * Errors encoding
  */
case class ErrorConsumerRecord(
  input: String,
  errorMessage: String
)

trait ErrorJsonEncoding extends DefaultJsonProtocol {

  implicit val errorConsumerRecord = jsonFormat2(ErrorConsumerRecord)

}

object ErrorJsonEncoding extends ErrorJsonEncoding

class SidecarServingProcessor(
  connector: RuntimeMeshConnector,
  managerConnector: ManagerConnector,
  applicationId: Long,
  ec: ExecutionContext
) extends ServingProcessor with ErrorJsonEncoding {

  implicit val EC = ec

  private var currentApplication: Option[(Application, LocalDateTime)] = None

  private def fetchApplication(): Future[Option[Application]] = {
    if (currentApplication.isEmpty || currentApplication.get._2.isBefore(LocalDateTime.now().minusSeconds(60))) {
      managerConnector.getApplications
        .map(apps => {
          val findApp = apps.find(a => a.id == applicationId)
          findApp match {
            case _ => findApp
            case Some(x) =>
              this.currentApplication = Option((x, LocalDateTime.now()))
              findApp
          }
        })
    } else {
      Future.successful(currentApplication.map(r => r._1))
    }
  }

  override def serve(input: Seq[String]): Future[Seq[String]] = {
    val data = KafkaDataFormat.toRequestData(input)

    fetchApplication().flatMap {
      case None =>
        Future.successful(
          input.map(i => ErrorConsumerRecord(i, s"Can't find application with id=$applicationId").toJson.compactPrint)
        )
      case Some(x) =>
        val cmd = ExecutionCommand(
          json = data,
          headers = Seq(),
          pipe = ToPipelineStages.applicationToStages.toStages(x, "/serve")
        )
        connector.execute(cmd).recover({
          case e: Throwable => ExecutionFailure(e.getMessage, StatusCodes.InternalServerError)
        }).map({
          case ExecutionFailure(err, _) =>
            val errors = input.map(i => ErrorConsumerRecord(i, err).toJson.compactPrint)
            errors
          case ExecutionSuccess(json) =>
            val outputs = KafkaDataFormat.toResponseRecords(json)
            outputs
        })
    }
  }
}

object SidecarServingProcessor {

  def apply(config: StreamingKafkaConfiguration)
    (implicit sys: ActorSystem, mat: ActorMaterializer): SidecarServingProcessor =
    SidecarServingProcessor(
      new HttpRuntimeMeshConnector(config.sidecar),
      new HttpManagerConnector(config.manager.host, config.manager.port),
      config.streaming.processorApplication
    )

  def apply(connector: RuntimeMeshConnector, managerConnector: ManagerConnector, applicationId: Long): SidecarServingProcessor = {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    new SidecarServingProcessor(
      connector,
      managerConnector,
      applicationId,
      ec
    )
  }

}
