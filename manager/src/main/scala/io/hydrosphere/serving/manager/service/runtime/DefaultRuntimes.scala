package io.hydrosphere.serving.manager.service.runtime

import io.hydrosphere.serving.manager.model.db.CreateRuntimeRequest
import io.hydrosphere.serving.model.api.ModelType

object DefaultRuntimes {
  final val dummies = List(
    CreateRuntimeRequest(
      name = "hydrosphere/serving-runtime-dummy",
      version = "latest",
      modelTypes = List(ModelType.Unknown().toTag),
      tags = List("dummy"),
      configParams = Map.empty
    )
  )

  final val sparkRuntimes = List(
    CreateRuntimeRequest(
      name = "hydrosphere/serving-runtime-spark",
      version = "2.0-latest",
      modelTypes = List(ModelType.Spark("2.0").toTag),
      tags = List("spark"),
      configParams = Map.empty
    ),
    CreateRuntimeRequest(
      name = "hydrosphere/serving-runtime-spark",
      version = "2.1-latest",
      modelTypes = List(ModelType.Spark("2.1").toTag),
      tags = List("spark"),
      configParams = Map.empty
    ),
    CreateRuntimeRequest(
      name = "hydrosphere/serving-runtime-spark",
      version = "2.2-latest",
      modelTypes = List(ModelType.Spark("2.2").toTag),
      tags = List("spark"),
      configParams = Map.empty
    )
  )

  final val tensorflowRuntimes = List(
    CreateRuntimeRequest(
      name = "hydrosphere/serving-runtime-tensorflow",
      version = "1.7.0-latest",
      modelTypes = List(ModelType.Tensorflow("1.7.0").toTag),
      tags = List("tensorflow"),
      configParams = Map.empty
    )
  )

  final val pythonRuntimes = List(
    CreateRuntimeRequest(
      name = "hydrosphere/serving-runtime-python",
      version = "3.6-latest",
      modelTypes = List(ModelType.PythonFunction("3.6").toTag),
      tags = List("python3"),
      configParams = Map.empty
    )
  )

  final val all = dummies ++ sparkRuntimes ++ tensorflowRuntimes ++ pythonRuntimes

  def getConfig(name: String): List[CreateRuntimeRequest] = {
    name match {
      case "dummy" => dummies
      case _ => all
    }
  }
}
