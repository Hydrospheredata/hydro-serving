package io.hydrosphere.serving.manager.domain.application

import cats.syntax.either._
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.model.api.MergeError
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
      val signatures = modelVariants.map(_.modelVersion.modelContract.predict.get)  // FIXME predict signature must be in the contract
      val signatureName = signatures.head.signatureName
      val isSameName = signatures.forall(_.signatureName == signatureName)
      if (isSameName) {
        val res = signatures.foldRight(Either.right[Seq[MergeError], ModelSignature](ModelSignature.defaultInstance)) {
          case (sig, Right(acc)) => ModelSignatureOps.merge(sig, acc)
          case (_, x) => x
        }
        res
          .right.map(_.withSignatureName(signatureName))
          .left.map(x => DomainError.invalidRequest(s"Errors while merging signatures: $x"))
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