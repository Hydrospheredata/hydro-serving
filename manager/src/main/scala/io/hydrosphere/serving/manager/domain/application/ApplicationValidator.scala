package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.model.api.ops.ModelSignatureOps

object ApplicationValidator {
  /**
    * Check if name matches with  `[a-zA-Z\-_\d]+` regexp
    *
    * @param name application name
    * @return
    */
  def name(name: String) = {
    val validName = raw"^[a-zA-Z\-_\d]+$$".r
    if (validName.pattern.matcher(name).matches()) {
      Some(name)
    } else {
      None
    }
  }

  /**
    * Checks if different ModelVariants are mergeable into single stage.
    *
    * @param modelVariants modelVariants
    * @return
    */
  def inferStageSignature(modelVariants: Seq[ModelVariant]): Either[DomainError, ModelSignature] = {
    if (modelVariants.isEmpty) {
      Left(DomainError.invalidRequest("Invalid application: no stages in the graph."))
    } else {
      val signatures = modelVariants.map(_.signature)
      val signatureName = signatures.head.signatureName
      val isSameName = signatures.forall(_.signatureName == signatureName)
      if (isSameName) {
        Right(
          signatures.foldRight(ModelSignature.defaultInstance) {
            case (sig1, sig2) => ModelSignatureOps.merge(sig1, sig2)
          }.withSignatureName(signatureName)
        )
      } else {
        Left(DomainError.invalidRequest(s"Model Versions ${modelVariants.map(x => x.modelVersion.model.name + ":" + x.modelVersion.modelVersion)} have different signature names"))
      }
    }
  }

  def inferPipelineSignature(name: String, graph: ApplicationExecutionGraph): Option[ModelSignature] = {
    if (graph.stages.isEmpty) {
      None
    } else {
      Some(
        ModelSignature(
          name,
          graph.stages.head.signature.inputs,
          graph.stages.last.signature.outputs
        )
      )
    }
  }
}