package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.mappers

import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.SparkModelMetadata

class LinearRegressionMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class GeneralizedLinearRegressionMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class DecisionTreeRegressionMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class RandomForestRegressionMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class GBTRegressionMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class AFTSurvivalRegressionMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class IsotonicRegressionMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }