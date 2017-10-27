package io.hydrosphere.serving.manager.service.modelfetcher.spark

import io.hydrosphere.serving.model_api._

trait SparkMlTypeMapper {
  type SchemaGenerator = SparkModelMetadata => List[ModelField]

  def inputSchema: SchemaGenerator
  def outputSchema: SchemaGenerator
  def labelSchema: Option[SchemaGenerator] = None

  private final def getDataFrame(sparkModelMetadata: SparkModelMetadata, schemaGenerator: SchemaGenerator) = {
    DataFrame(schemaGenerator(sparkModelMetadata))
  }

  final def input(sparkModelMetadata: SparkModelMetadata): DataFrame = getDataFrame(sparkModelMetadata, inputSchema)
  final def output(sparkModelMetadata: SparkModelMetadata): DataFrame = getDataFrame(sparkModelMetadata, outputSchema)

  final def labels(sparkModelMetadata: SparkModelMetadata): Option[DataFrame] = labelSchema.map(getDataFrame(sparkModelMetadata,_))
}

object SparkMlTypeMapper {
  def sparkVector: FMatrix = FMatrix.varvec(FDouble)

  def apply(sparkModelMetadata: SparkModelMetadata): SparkMlTypeMapper = {
    sparkModelMetadata.`class` match {
      case "org.apache.spark.ml.feature.HashingTF" => HashingTFMapper
      case "org.apache.spark.ml.feature.IDF" => IDFMapper
      case "org.apache.spark.ml.feature.Word2VecModel" => Word2VecMapper
      case "org.apache.spark.ml.feature.CountVectorizerModel" => CountVectorizerMapper
      case "org.apache.spark.ml.feature.Tokenizer" => TokenizerMapper
      case "org.apache.spark.ml.feature.RegexTokenizer" => TokenizerMapper
      case "org.apache.spark.ml.feature.StopWordsRemover" => StopWordsRemoverMapper
      case "org.apache.spark.ml.feature.NGram" => NGramMapper
      case "org.apache.spark.ml.feature.Binarizer" => BinarizerMapper
      case "org.apache.spark.ml.feature.PCAModel" => PCAMapper
      case "org.apache.spark.ml.feature.PolynomialExpansion" => PolynomialExpansionMapper
      case "org.apache.spark.ml.feature.DCT" => DCTMapper
      case "org.apache.spark.ml.feature.StringIndexerModel" => StringIndexerMapper
      case "org.apache.spark.ml.feature.IndexToString" => IndexToStringMapper
      case "org.apache.spark.ml.feature.OneHotEncoder" => OneHotEncoderMapper
      case "org.apache.spark.ml.feature.VectorIndexerModel" => VectorIndexerMapper
      case "org.apache.spark.ml.feature.Interaction" => InteractionMapper
      case "org.apache.spark.ml.feature.Normalizer" => NormalizerMapper
      case "org.apache.spark.ml.feature.StandardScalerModel" => StandardScalerMapper
      case "org.apache.spark.ml.feature.MinMaxScalerModel" => MinMaxScalerMapper
      case "org.apache.spark.ml.feature.MaxAbsScalerModel" => MaxAbsScalerMapper
      case "org.apache.spark.ml.feature.Bucketizer" => BucketizerMapper
      case "org.apache.spark.ml.feature.ElementwiseProduct" => ElementwiseProductMapper
      case "org.apache.spark.ml.feature.VectorAssembler" => VectorAssemblerMapper
      case "org.apache.spark.ml.feature.VectorSlicer" => VectorSlicerMapper
      case "org.apache.spark.ml.feature.ChiSqSelectorModel" => ChiSqSelectorMapper
      case "org.apache.spark.ml.feature.BucketedRandomProjectionLSHModel" => BucketedRandomProjectionLSHMapper
      case "org.apache.spark.ml.feature.MinHashLSHModel" => MinHashLSH

      case "org.apache.spark.ml.classification.LogisticRegressionModel" => LogisticRegression
      case "org.apache.spark.ml.classification.DecisionTreeClassificationModel" => DecisionTreeClassifierMapper
      case "org.apache.spark.ml.classification.RandomForestClassificationModel" => RandomForestClassifierMapper
      case "org.apache.spark.ml.classification.GBTClassificationModel" => GBTClassifierMapper
      case "org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel" => MultilayerPerceptronClassificationMapper
      case "org.apache.spark.ml.classification.LinearSVCModel" => LinearSVCMapper
      //case "org.apache.spark.ml.classification.OneVsRestModel" => OneVsRestMapper TODO needs base classifier
      case "org.apache.spark.ml.classification.NaiveBayesModel" => NaiveBayesMapper

      case "org.apache.spark.ml.regression.LinearRegressionModel" => LinearRegressionMapper
      case "org.apache.spark.ml.regression.GeneralizedLinearRegressionModel" => GeneralizedLinearRegressionMapper
      case "org.apache.spark.ml.regression.DecisionTreeRegressionModel" => DecisionTreeRegressionMapper
      case "org.apache.spark.ml.regression.RandomForestRegressionModel" => RandomForestRegressionMapper
      case "org.apache.spark.ml.regression.GBTRegressionModel" => GBTRegressionMapper
      case "org.apache.spark.ml.regression.AFTSurvivalRegressionModel" => AFTSurvivalRegressionMapper
      case "org.apache.spark.ml.regression.IsotonicRegressionModel" => IsotonicRegressionMapper

      case "org.apache.spark.ml.clustering.KMeansModel" => KMeansMapper
      case _=> UntypedMapper
    }
  }

  def getInputSchema(metadata: SparkModelMetadata): DataFrame = SparkMlTypeMapper(metadata).input(metadata)

  def getOutputSchema(metadata: SparkModelMetadata): DataFrame = SparkMlTypeMapper(metadata).output(metadata)

  def getLabels(metadata: SparkModelMetadata): Option[DataFrame] = SparkMlTypeMapper(metadata).labels(metadata)
}

trait FeaturesOutputMapper extends SparkMlTypeMapper {
  def featuresType(sparkModelMetadata: SparkModelMetadata): FieldType
  def outputType(sparkModelMetadata: SparkModelMetadata): FieldType

  final def inputSchema: SchemaGenerator = { m =>
    List(ModelField(m.getParam("featuresCol").get, featuresType(m)))
  }

  final def outputSchema: SchemaGenerator= { m =>
    List(ModelField(m.getParam("outputCol").get, outputType(m)))
  }
}
trait InputOutputMapper extends SparkMlTypeMapper{
  def inputType(sparkModelMetadata: SparkModelMetadata): FieldType
  def outputType(sparkModelMetadata: SparkModelMetadata): FieldType

  final def inputSchema: SchemaGenerator = { m =>
    List(ModelField(m.getParam("inputCol").get, inputType(m)))
  }

  final def outputSchema: SchemaGenerator= { m =>
    List(ModelField(m.getParam("outputCol").get, outputType(m)))
  }
}
trait PredictorMapper extends SparkMlTypeMapper {
  def featuresType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
  def predictionType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble

  override def labelSchema: Option[SchemaGenerator] = Some({ m=>
    List(
      ModelField.untyped(m.getParam[String]("labelCol").get)
    )
  })

  override def inputSchema: SchemaGenerator = { m =>
    List(ModelField(m.getParam[String]("featuresCol").get, featuresType(m)))
  }

  override def outputSchema: SchemaGenerator = { m =>
    List(ModelField(m.getParam[String]("predictionCol").get, predictionType(m)))
  }
}
trait ClassifierMapper extends PredictorMapper {
  def rawPredictionType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputSchema: SchemaGenerator = { m =>
    super.outputSchema.apply(m) ++ List(
      ModelField(m.getParam("rawPredictionCol").get, rawPredictionType(m))
    )
  }
}
trait ProbabilisticClassifierMapper extends ClassifierMapper {
  def probabilityType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputSchema: SchemaGenerator = { m =>
    super.outputSchema.apply(m) ++ List(
      ModelField(m.getParam("probabilityCol").get, probabilityType(m))
    )
  }
}

object HashingTFMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FString)

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix(
    FDouble,
    List(sparkModelMetadata.numFeatures.getOrElse(-1).toLong)
  )
}
object IDFMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object Word2VecMapper extends InputOutputMapper {
  def inputType(metadata: SparkModelMetadata): FieldType = FString

  def outputType(metadata: SparkModelMetadata): FieldType =
    FMatrix(
      FDouble,
      List(
        metadata.getParam[Double]("vectorSize").getOrElse(-1.0).toLong // NB Spark uses doubles to store vector length
      )
    )
}
object CountVectorizerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FString)

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object TokenizerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FString

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FString)
}
object StopWordsRemoverMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FString)

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FString)
}
object NGramMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FString)

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FString)
}
object BinarizerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble
}
object PCAMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix(
    FDouble,
    List(sparkModelMetadata.getParam[Double]("k").getOrElse(-1.0).toLong)
  )
}
object PolynomialExpansionMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object DCTMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object StringIndexerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FString

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble
}
object IndexToStringMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FString
}
object OneHotEncoderMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FString

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object VectorIndexerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble
}
object InteractionMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object NormalizerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object StandardScalerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object MinMaxScalerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object MaxAbsScalerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object BucketizerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FDouble
}
object ElementwiseProductMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object VectorAssemblerMapper extends SparkMlTypeMapper {
  override def inputSchema: VectorAssemblerMapper.SchemaGenerator = { m =>
    m.getParam[Seq[String]]("inputCols")
      .map(_.map(ModelField.untyped))
      .getOrElse(
        List(ModelField.untyped("input"))
      ).toList
  }

  override def outputSchema: VectorAssemblerMapper.SchemaGenerator = { m =>
    List(
      ModelField(m.getParam("outputCol").get, SparkMlTypeMapper.sparkVector)
    )
  }
}
object VectorSlicerMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FAnyScalar)

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = FMatrix.varvec(FAnyScalar)
}
object ChiSqSelectorMapper extends FeaturesOutputMapper {
  override def featuresType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object BucketedRandomProjectionLSHMapper extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}
object MinHashLSH extends InputOutputMapper {
  override def inputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector

  override def outputType(sparkModelMetadata: SparkModelMetadata): FieldType = SparkMlTypeMapper.sparkVector
}

object LogisticRegression extends PredictorMapper { }
object DecisionTreeClassifierMapper extends ProbabilisticClassifierMapper { }
object RandomForestClassifierMapper extends ProbabilisticClassifierMapper { }
object GBTClassifierMapper extends ProbabilisticClassifierMapper { }
object MultilayerPerceptronClassificationMapper extends PredictorMapper { }
object LinearSVCMapper extends ClassifierMapper { }
object OneVsRestMapper extends ClassifierMapper { }
object NaiveBayesMapper extends ProbabilisticClassifierMapper { }

object LinearRegressionMapper extends PredictorMapper { }
object GeneralizedLinearRegressionMapper extends PredictorMapper { }
object DecisionTreeRegressionMapper extends PredictorMapper { }
object RandomForestRegressionMapper extends PredictorMapper { }
object GBTRegressionMapper extends PredictorMapper { }
object AFTSurvivalRegressionMapper extends PredictorMapper { }
object IsotonicRegressionMapper extends PredictorMapper { }

object KMeansMapper extends PredictorMapper {
  override def labelSchema: Option[KMeansMapper.SchemaGenerator] = None
  override def predictionType(sparkModelMetadata: SparkModelMetadata): FieldType = FInteger
}

object UntypedMapper extends SparkMlTypeMapper {
  private[this] val inputCols = Array("inputCol", "featuresCol")
  private[this] val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  private[this] val labelCols = Array("labelCol")

  override def inputSchema: UntypedMapper.SchemaGenerator = {m =>
    inputCols
      .map(m.getParam[String])
      .map(opt => opt.map(ModelField.untyped))
      .filter(_.isDefined)
      .map(_.get)
      .toList
  }

  override def outputSchema: UntypedMapper.SchemaGenerator = { m =>
    outputCols
      .map(m.getParam[String])
      .map(opt => opt.map(ModelField.untyped))
      .filter(_.isDefined)
      .map(_.get)
      .toList
  }
}
