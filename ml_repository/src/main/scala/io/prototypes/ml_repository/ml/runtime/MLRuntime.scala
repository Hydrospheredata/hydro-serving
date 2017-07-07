package io.prototypes.ml_repository.ml.runtime

import io.prototypes.ml_repository.ml.Model

/**
  * Created by Bulat on 26.05.2017.
  */
trait MLRuntime {
  def getModel(directory: String): Option[Model]
  def getModels: Seq[Model]
}
