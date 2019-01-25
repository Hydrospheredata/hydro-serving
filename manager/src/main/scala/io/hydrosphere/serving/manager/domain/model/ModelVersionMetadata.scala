package io.hydrosphere.serving.manager.domain.model

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType
import io.hydrosphere.serving.manager.domain.DomainError.InvalidRequest
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage

case class ModelVersionMetadata(
  modelName: String,
  contract: ModelContract,
  profileTypes: Map[String, DataProfileType],
  runtime: DockerImage,
  hostSelector: Option[HostSelector]
)

object ModelVersionMetadata {
  def fromModel(modelContract: ModelContract, upload: ModelUploadMetadata, hs: Option[HostSelector]) = {
    val name = upload.name.getOrElse(modelContract.modelName)
    val contract = upload.contract.getOrElse(modelContract).copy(modelName = name)
    ModelVersionMetadata(
      modelName = name,
      contract = contract,
      profileTypes = upload.profileTypes.getOrElse(Map.empty),
      runtime = upload.runtime,
      hostSelector = hs
    )
  }


  def validateContract(upload: ModelVersionMetadata): Either[InvalidRequest, Unit] = {
    if (upload.contract.signatures.isEmpty) {
      Left(InvalidRequest("The model has no signatures"))
    } else {
      val inputsOk = upload.contract.signatures.forall(_.inputs.nonEmpty)
      val outputsOk = upload.contract.signatures.forall(_.outputs.nonEmpty)
      if (inputsOk && outputsOk) {
        Right(())
      } else {
        Left(InvalidRequest(s"Error during signature validation. (inputsOk=$inputsOk, outputsOk=$outputsOk)"))
      }
    }
  }
}