package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.tensorflow

import io.hydrosphere.serving.tensorflow.types.DataType

object TypeMapper {
  val nameToDtypes = Map(
    "invalid" -> DataType.DT_INVALID,
    "variant" -> DataType.DT_VARIANT,
    "bool" -> DataType.DT_BOOL,
    "string" -> DataType.DT_STRING,

    "bfloat16" -> DataType.DT_BFLOAT16,
    "float16" -> DataType.DT_HALF,
    "half" -> DataType.DT_HALF,
    "float32" -> DataType.DT_FLOAT,
    "float64" -> DataType.DT_DOUBLE,

    "int8" -> DataType.DT_INT8,
    "int16" -> DataType.DT_INT16,
    "int32" -> DataType.DT_INT32,
    "int64" -> DataType.DT_INT64,

    "uint8" -> DataType.DT_UINT8,
    "uint16" -> DataType.DT_UINT16,
    "uint32" -> DataType.DT_UINT32,
    "uint64" -> DataType.DT_UINT64,

    "qint8" -> DataType.DT_QINT8,
    "qint16" -> DataType.DT_QINT16,
    "qint32" -> DataType.DT_QINT32,

    "quint8" -> DataType.DT_QUINT8,
    "quint16" -> DataType.DT_QUINT16,

    "complex64" -> DataType.DT_COMPLEX64,
    "complex128" -> DataType.DT_COMPLEX128,
  )

  val dtypesToNames = nameToDtypes.filterKeys(_ != "half").map(_.swap)

  def toType(dtype: String): DataType = {
    nameToDtypes.getOrElse(dtype, DataType.DT_INVALID)
  }

  def toName(dtype: DataType): String = {
    dtypesToNames.getOrElse(dtype, "invalid")
  }

  def isSupportedType(name: String): Boolean = {
    nameToDtypes.contains(name)
  }

  def isSupportedType(dataType: DataType): Boolean = {
    dtypesToNames.contains(dataType)
  }
}
