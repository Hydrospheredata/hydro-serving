package io.hydrosphere.serving.manager.service.modelfetcher

import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType

case class FieldInfo(dataType: DataType, shape: TensorShape)
