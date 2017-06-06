package io.prototypes.ml_repository.runtime

import java.io.File

import io.prototypes.ml_repository.Model

/**
  * Created by Bulat on 26.05.2017.
  */
trait MLRuntime {
  def getModel(directory: File): Option[Model]
  def getModels: Seq[Model]
}
