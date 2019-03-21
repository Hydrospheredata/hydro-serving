package io.hydrosphere.serving.manager.domain.model

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
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
    upload.contract.predict match {
      case None => Left(InvalidRequest("The model has no prediction signature"))
      case Some(predictSignature) =>
        val inputsNotEmpty = predictSignature.inputs.nonEmpty
        val outputsNotEmpty = predictSignature.outputs.nonEmpty
        val inputErrors = predictSignature.inputs.map(validateField)
        val outputErrors = predictSignature.outputs.map(validateField)
        if (inputsNotEmpty && outputsNotEmpty) {
          Right(())
        } else {
          Left(InvalidRequest(s"Error during prediction signature validation. " +
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