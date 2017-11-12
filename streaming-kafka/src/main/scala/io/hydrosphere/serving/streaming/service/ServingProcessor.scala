package io.hydrosphere.serving.streaming

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.config.SidecarConfig
import io.hydrosphere.serving.connector._
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
  applicationId: Long,
  ec: ExecutionContext
) extends ServingProcessor with ErrorJsonEncoding {

  implicit val EC = ec


  override def serve(input: Seq[String]): Future[Seq[String]] = {
    val data = KafkaDataFormat.toRequestData(input)
    val cmd = ExecutionCommand(
      json = data,
      headers = Seq(),
      pipe = Seq(ExecutionUnit(
        serviceName = serviceName,
        servicePath = "/serve"
      ))
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

object SidecarServingProcessor {

  def apply(sidecarConfig: SidecarConfig, applicationId: Long)
    (implicit sys: ActorSystem, mat: ActorMaterializer): SidecarServingProcessor =
    SidecarServingProcessor(new HttpRuntimeMeshConnector(sidecarConfig), applicationId)

  def apply(connector: RuntimeMeshConnector, applicationId: Long): SidecarServingProcessor = {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    new SidecarServingProcessor(connector, applicationId, ec)
  }

}
