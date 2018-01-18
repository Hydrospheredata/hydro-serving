package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata

class LogisticRegression(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class DecisionTreeClassifierMapper(m: SparkModelMetadata)  extends ProbabilisticClassifierMapper(m) { }

class RandomForestClassifierMapper(m: SparkModelMetadata)  extends ProbabilisticClassifierMapper(m) { }

class GBTClassifierMapper(m: SparkModelMetadata)  extends ProbabilisticClassifierMapper(m) { }

class MultilayerPerceptronClassificationMapper(m: SparkModelMetadata)  extends PredictorMapper(m) { }

class LinearSVCMapper(m: SparkModelMetadata)  extends ClassifierMapper(m) { }

class OneVsRestMapper(m: SparkModelMetadata)  extends ClassifierMapper(m) { }

class NaiveBayesMapper(m: SparkModelMetadata)  extends ProbabilisticClassifierMapper(m) { }
