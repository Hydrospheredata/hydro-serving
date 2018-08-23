package io.hydrosphere.serving.manager.config

import io.hydrosphere.serving.manager.model.db.CreateRuntimeRequest
import io.hydrosphere.serving.manager.service.runtime.DefaultRuntimes

sealed trait RuntimePackConfig {
  def toRuntimePack: List[CreateRuntimeRequest]
}

object RuntimePackConfig {
  case class Dummies() extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.dummies
  }

  case class Tensorflow() extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.tensorflowRuntimes
  }

  case class Python() extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.pythonRuntimes
  }

  case class Spark() extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.sparkRuntimes
  }

  case class All() extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.all
  }

}
