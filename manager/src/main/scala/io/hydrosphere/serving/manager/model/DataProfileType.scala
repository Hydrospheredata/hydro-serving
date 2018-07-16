package io.hydrosphere.serving.manager.model

object DataProfileType extends Enumeration {
  type DataProfileType = Value
  val NONE = DataProfileType
  val CATEGORICAL, NOMINAL, ORIDNAL = DataProfileType
  val NUMERICAL, CONTINUOUS, INTERVAL, RATIO = DataProfileType
  val IMAGE = DataProfileType
  val MUSIC = DataProfileType
  val TEXT = DataProfileType

  def toGrpc(dtype: DataProfileType) = ???
}
