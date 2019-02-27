package io.hydrosphere.serving.manager.infrastructure.storage.fetchers

import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType

case class FieldInfo(dataType: DataType, shape: TensorShape)
