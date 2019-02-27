package io.hydrosphere.serving.manager.domain.model

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType
import io.hydrosphere.serving.manager.domain.DomainError.InvalidRequest
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.FetcherResult

case class ModelVersionMetadata(
  modelName: String,
  contract: ModelContract,
  profileTypes: Map[String, DataProfileType],
  runtime: DockerImage,
  hostSelector: Option[HostSelector],
  installCommand: Option[String],
  metadata: Map[String, String]
)

object ModelVersionMetadata {
  def combineMetadata(fetcherResult: Option[FetcherResult], upload: ModelUploadMetadata, hs: Option[HostSelector]) = {
    val contract = upload.contract
      .orElse(fetcherResult.map(_.modelContract))
      .getOrElse(ModelContract.defaultInstance)
      .copy(modelName = upload.name)

    val metadata = fetcherResult.map(_.metadata).getOrElse(Map.empty) ++ upload.metadata.getOrElse(Map.empty)

    ModelVersionMetadata(
      modelName = upload.name,
      contract = contract,
      profileTypes = upload.profileTypes.getOrElse(Map.empty),
      runtime = upload.runtime,
      hostSelector = hs,
      installCommand = upload.installCommand,
      metadata = metadata
    )
  }


  def validateContract(upload: ModelVersionMetadata): Either[InvalidRequest, Unit] = {
    if (upload.contract.signatures.isEmpty) {
      Left(InvalidRequest("The model has no signatures"))
    } else {
      val inputsNotEmpty = upload.contract.signatures.forall(_.inputs.nonEmpty)
      val outputsNotEmpty = upload.contract.signatures.forall(_.outputs.nonEmpty)
      val inputErrors = upload.contract.signatures.flatMap(_.inputs.flatMap(validateField))
      val outputErrors = upload.contract.signatures.flatMap(_.outputs.flatMap(validateField))
      if (inputsNotEmpty && outputsNotEmpty) {
        Right(())
      } else {
        Left(InvalidRequest(s"Error during signature validation. " +
          s"(inputsNotEmpty=$inputsNotEmpty, " +
          s"outputsNotEmpty=$outputsNotEmpty," +
          s"inputErrors=$inputErrors, " +
          s"outputErrors=$outputErrors)"))
      }
    }
  }

  def validateField(modelField: ModelField): List[String] = {
    modelField.typeOrSubfields match {
      case ModelField.TypeOrSubfields.Dtype(dtype) =>
        if (dtype.isDtInvalid || dtype.isUnrecognized) {
          List(s"${modelField.name}: Invalid Dtype $dtype")
        } else {
          List.empty
        }
      case ModelField.TypeOrSubfields.Subfields(subfields) =>
        val results = subfields.data.map(validateField)
        results.foldLeft(List.empty[String]) {
          case (a, b) => a ++ b
        }
      case ModelField.TypeOrSubfields.Empty => List(s"${modelField.name}: Type cannot be empty.")
    }
  }
}