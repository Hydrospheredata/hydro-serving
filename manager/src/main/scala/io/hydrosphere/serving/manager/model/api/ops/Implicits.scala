package io.hydrosphere.serving.manager.model.api.ops

trait Implicits
  extends ModelContractOps
  with ModelSignatureOps
  with ModelFieldOps {}

object Implicits extends Implicits
