# Hydrosphere Serving

[![Join the chat at https://gitter.im/Hydrospheredata/hydro-serving](https://badges.gitter.im/Hydrospheredata/hydro-serving.svg)](https://gitter.im/Hydrospheredata/hydro-serving?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![](https://img.shields.io/badge/documentation-latest-af1a97.svg)](https://hydrosphere.io/serving-docs/) 

Homepage: https://hydrosphere.io/serving  
Installation guide: https://hydrosphere.io/serving-docs/latest/install  
Getting started: https://hydrosphere.io/serving-docs/latest/tutorials

---

Hydrosphere.io is the first open source platform for Data Science Management automation. 
It delivers reliability, scalability and observability for machine learning and AI applications in production.
Hydrosphere.io automates deployment and serving ML models, monitoring and profiling of production traffic, 
monitoring of models performance, data subsampling and model retraining.

The platform makes more Data Science and less data plumbing and tinkering happen.

Hydrosphere Serving enables you to get your models up and running in an instant, 
on just about any infrastructure and using any of the available machine learning toolkits. 
It lets you monitor your models’ performance, analyse their inputs 
(for example, determine whether there is recently an increased number of outliers or not),
observe models’ inference on given data and so on.

Features:
* **Serverless** user experience in your data center or public cloud. 
* **Plumbing**. Automatic generation of Protobuf contracts for REST, gRPC and Streaming Kafka API from the model metadata
* Safe experiments and models warm up on **shadowed or canary traffic**
* Unified across ML frameworks
* Automatic data profiling and statistical check of the model quality 
* Immutable model versioning
* Agnostic to training pipeline and notebook environment 
* **Multi-framework Pipelines** (e.g. Scikit-learn -> Spark ML -> TensorFlow pipeline)
* Out of the box tuned and optimized Serving Runtimes
* Models optimization for Serving

#### Check UI - explore, deploy and test models

![Hydrosphere Serving Deploy Models](https://media.giphy.com/media/KyEVbxQEr4IGLuaQlR/giphy.gif)

![Hydrosphere Serving Create an AI application](https://media.giphy.com/media/1dHWK2HJjdheyqB8lZ/giphy.gif)

![Hydrosphere Serving Test ML Models](https://media.giphy.com/media/2A67Wd88zQTcZk4lEs/giphy.gif)

## Related repositories
 * Runtimes:
   * Tensorflow: https://github.com/Hydrospheredata/hydro-serving-tensorflow
   * Python: https://github.com/Hydrospheredata/hydro-serving-python
   * Spark: https://github.com/Hydrospheredata/hydro-serving-spark
     * Spark local inference implementation: https://github.com/Hydrospheredata/spark-ml-serving
 * Protobuf messages: https://github.com/Hydrospheredata/hydro-serving-protos
 * Manager service: https://github.com/Hydrospheredata/hydro-serving-manager
 * Gateway service: https://github.com/Hydrospheredata/hydro-serving-gateway
 * Example models: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models
