# Hydro-serving

[![Join the chat at https://gitter.im/Hydrospheredata/hydro-serving](https://badges.gitter.im/Hydrospheredata/hydro-serving.svg)](https://gitter.im/Hydrospheredata/hydro-serving?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Hydro Serving is a Machine Learning Serving cluster. 

Features:
* **Serverless** user experience in your data center or public cloud. 
* State of the art architecture based on **Service mesh** powered by Envoy proxy  
* **Plumbing**. Automatic generation of Protobuf contracts for REST, gRPC and Streaming Kafka API from the model metadata
* Safe experiments and models warm up on **shadowed or canary traffic**
* Unified across ML frameworks
* Automatic data profiling and statistical check of the model quality 
* Immutable model versioning
* Agnostic to training pipeline and notebook environment 
* **Multi-framework Pipelines** (e.g. Scikit-learn -> Spark ML -> TensorFlow pipeline)
* Out of the box tuned and optimized Serving Runtimes
* Models optimization for Serving

## Quick Start

#### Run
```
#Clone integration repository
git clone https://github.com/Hydrospheredata/hydro-serving-example

cd ./hydro-serving-example
docker-compose up
```

Go to UI: http://localhost/

#### Deploy your first model

```bash
#install Serving CLI
pip install hs

cd stateful_lstm_example/pipeline/lstm
hs upload
```

#### Check UI - explore, deploy and test models

![Hydrosphere Serving in Action](https://media.giphy.com/media/KyEVbxQEr4IGLuaQlR/giphy.gif)

## Model Server Architecture
![Image](docs/images/high-level-architecture.png)


## Related repositories
 * Runtimes:
   * Tensorflow: https://github.com/Hydrospheredata/hydro-serving-tensorflow
   * Python: https://github.com/Hydrospheredata/hydro-serving-python
   * Spark: https://github.com/Hydrospheredata/hydro-serving-spark
     * Spark local inference implementation: https://github.com/Hydrospheredata/spark-ml-serving
 * Protobuf messages: https://github.com/Hydrospheredata/hydro-serving-protos
 * Sidecar implementation: https://github.com/Hydrospheredata/hydro-serving-sidecar
 * Example models: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models
