package io.hydrosphere.serving.manager.config

import io.hydrosphere.serving.manager.model.db.CreateRuntimeRequest
import io.hydrosphere.serving.manager.service.runtime.DefaultRuntimes

sealed trait RuntimePackConfig {
  def toRuntimePack: List[CreateRuntimeRequest]
}

object RuntimePackConfig {
  case object Dummies extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.dummies
  }

  case object Tensorflow extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.tensorflowRuntimes
  }

  case object Python extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.pythonRuntimes
  }

  case object Spark extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.sparkRuntimes
  }

  case object All extends RuntimePackConfig {
    override def toRuntimePack: List[CreateRuntimeRequest] = DefaultRuntimes.all
  }

}
