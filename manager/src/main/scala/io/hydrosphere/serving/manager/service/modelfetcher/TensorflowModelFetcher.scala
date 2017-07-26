package io.hydrosphere.serving.manager.service.modelfetcher
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.{Model, RuntimeType, SchematicRuntimeType}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.tensorflow.framework.meta_graph.MetaGraphDef
/**
  * Created by bulat on 24/07/2017.
  */
object TensorflowModelFetcher extends ModelFetcher {
  override def fetch(source: ModelSource, directory: String): Option[Model] = {
    try {
      val fullPath = s"${source.getSourcePrefix()}:/$directory"
      val metagraph = MetaGraphDef.parseFrom(Files.newInputStream(Paths.get("")))
      val inputs = metagraph.signatureDef("default").inputs.keys.toList
      val outputs = metagraph.signatureDef("default").outputs.keys.toList
      Some(
        Model(
          -1,
          directory,
          fullPath,
          Some(
            new SchematicRuntimeType("tensorflow", "1.0")
          ),
          None,
          outputs,
          inputs,
          LocalDateTime.now(),
          LocalDateTime.now()
        )
      )
    } catch {
      case e: Exception =>
        None
    }
  }
}
