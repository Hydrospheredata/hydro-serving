package io.hydrosphere.serving.manager.api.http.controller.model

import java.nio.file.{Files, StandardOpenOption}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.api.http.controller.GenericController
import io.hydrosphere.serving.manager.domain.model.{Model, ModelService}
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionService}
import io.hydrosphere.serving.manager.service.aggregated_info.AggregatedModelInfo
import io.swagger.annotations._
import javax.ws.rs.Path

import scala.concurrent.Future
import scala.concurrent.duration._

@Path("/api/v2/model")
@Api(produces = "application/json", tags = Array("Model and Model Versions"))
class ModelController(
  modelManagementService: ModelService,
  modelVersionManagementService: ModelVersionService
)(
  implicit val system: ActorSystem,
  val materializer: ActorMaterializer,
)
  extends GenericController {
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(10.minutes)

  @Path("/")
  @ApiOperation(value = "listModels", notes = "listModels", nickname = "listModels", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[AggregatedModelInfo], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModels = pathPrefix("model") {
    get {
      completeF(???)
    }
  }

  @Path("/{modelId}")
  @ApiOperation(value = "getModel", notes = "getModel", nickname = "getModel", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[AggregatedModelInfo]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getModel = pathPrefix("model" / LongNumber) { id =>
    get {
      completeFRes(???)
    }
  }

  @Path("/upload")
  @ApiOperation(value = "Upload and release a model", notes = "Upload and release a model", nickname = "uploadModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "CreateOrUpdateModelRequest", required = true,
      dataTypeClass = classOf[Object], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[ModelVersion]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def uploadModel = pathPrefix("model" / "upload") {
    post {
      entity(as[Multipart.FormData]) { (formdata: Multipart.FormData) ⇒
        val fileNamesFuture = formdata.parts.flatMapConcat { p =>
          logger.debug(s"Got part. Name: ${p.name} Filename: ${p.filename}")
          p.name match {
            case Entities.`modelType` if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => UploadModelType(r))

            case Entities.`modelContract` if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(b => ModelContract.parseFrom(b.toByteBuffer.array()))
                .map(p => UploadContract(p))

            case Entities.`modelDescription` if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => UploadDescription(description = r))

            case Entities.`modelName` if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => UploadModelName(r))

            case Entities.`payload` if p.filename.isDefined =>
              val filename = p.filename.get
              val tempPath = Files.createTempFile("upload", filename)
              p.entity.dataBytes
                .map { fileBytes =>
                  Files.write(tempPath, fileBytes.toArray, StandardOpenOption.APPEND)
                  UploadTarball(tempPath)
                }

            case Entities.`remotePayload` if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => UploadRemotePayload(r))

            case _ =>
              logger.warn(s"Unknown part. Name: ${p.name} Filename: ${p.filename}")
              p.entity.dataBytes.map { _ =>
                UnknownPart(p.name)
              }
          }
        }

        val entities = fileNamesFuture.runFold(List.empty[UploadedEntity]) {
          case (a, b) => a :+ b
        }

        def uploadModel(dd: Future[List[UploadedEntity]]) = {
          dd.map(ModelUpload.fromUploadEntities).flatMap {
            case Left(a) => Future.successful(Left(a))
            case Right(b) => modelManagementService.uploadModel(b)
          }
        }

        val taskStatus = for {
          taskResponse <- EitherT(uploadModel(entities))
        } yield taskResponse

        completeFRes(taskStatus.value)
      }
    }
  }

  @Path("version/{modelId}/last")
  @ApiOperation(value = "lastModelVersions", notes = "lastModelVersions", nickname = "lastModelVersions", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "maximum", required = false, dataType = "integer", paramType = "query", value = "maximum", defaultValue = "10")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelVersion", response = classOf[ModelVersion], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def lastModelVersions = get {
    pathPrefix("model" / "version" / LongNumber / "last") { s =>
      parameters('maximum.as[Int]) { maximum =>
        completeF(
          modelVersionManagementService.lastModelVersionByModelId(s, maximum)
        )
      }
    }
  }

  @Path("version")
  @ApiOperation(value = "All ModelVersion", notes = "All ModelVersion", nickname = "allModelVersions", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelVersion", response = classOf[ModelVersion], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def allModelVersions = pathPrefix("model" / "version") {
    get {
      completeF(
        aggregatedInfoUtilityService.allModelVersions
      )
    }
  }

  @Path("/{modelId}")
  @ApiOperation(value = "Delete model if not in app", notes = "Fails if any version of the model is deployed", nickname = "deleteModel", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteModel = pathPrefix("model" / LongNumber) { modelId =>
    delete {
      completeFRes {
        aggregatedInfoUtilityService.deleteModel(modelId)
      }
    }
  }


  val routes: Route = listModels ~ getModel ~ uploadModel ~
    lastModelVersions ~ allModelVersions ~ deleteModel
}