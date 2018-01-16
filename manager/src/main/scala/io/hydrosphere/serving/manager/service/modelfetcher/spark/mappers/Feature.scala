package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.tensorflow.types.DataType.{DT_DOUBLE, DT_STRING, DT_VARIANT}
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers.SparkMlTypeMapper._
import io.hydrosphere.serving.model_api.ModelContractBuilders

class HashingTFMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class IDFMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class Word2VecMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  def inputType(metadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)

  def outputType(metadata: SparkModelMetadata): TypeDescription = fixedVec(
    DataType.DT_DOUBLE,
    metadata.getParam[Double]("vectorSize").getOrElse(-1.0).toLong // NB Spark uses doubles to store vector length
  )
}
class CountVectorizerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class TokenizerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_STRING)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)
}
class StopWordsRemoverMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)
}
class NGramMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DT_STRING)
}
class BinarizerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)
}
class PCAMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = fixedVec(
    DataType.DT_DOUBLE,
    sparkModelMetadata.getParam[Double]("k").getOrElse(-1.0).toLong
  )
}
class PolynomialExpansionMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class DCTMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class StringIndexerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_STRING)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)
}
class IndexToStringMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_STRING)
}
class OneHotEncoderMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_STRING)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class VectorIndexerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)
}
class InteractionMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class NormalizerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class StandardScalerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class MinMaxScalerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class MaxAbsScalerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class BucketizerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)
}
class ElementwiseProductMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class VectorAssemblerMapper(m: SparkModelMetadata)  extends SparkMlTypeMapper(m) {
  override def inputSchema: List[ModelField] = {
    m.getParam[Seq[String]]("inputCols")
      .map{
        _.map{ col =>
          ModelContractBuilders.simpleTensorModelField(col, DT_VARIANT, Some(Seq.empty), unknownRank = true)
        }
      }
      .getOrElse(
        List(ModelField())
      ).toList
  }

  override def outputSchema: List[ModelField] = {
    List(
      constructField(m.getParam("outputCol").get, SparkMlTypeMapper.featuresVec(m))
    )
  }
}
class VectorSlicerMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_VARIANT)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_VARIANT)
}
class ChiSqSelectorMapper(m: SparkModelMetadata)  extends FeaturesOutputMapper(m) {
  override def featuresType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class BucketedRandomProjectionLSHMapper(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
class MinHashLSH(m: SparkModelMetadata)  extends InputOutputMapper(m) {
  override def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)

  override def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription = varVec(DataType.DT_DOUBLE)
}
