# Hydro-serving

[![Join the chat at https://gitter.im/Hydrospheredata/hydro-serving](https://badges.gitter.im/Hydrospheredata/hydro-serving.svg)](https://gitter.im/Hydrospheredata/hydro-serving?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Hydro Serving is a Machine Learning Serving cluster. 

Main Features:
* **Serverless**. It is as easy as AWS Lambda in your data center or VPC. 
* **Multi-framework Pipelines** (e.g. Scikit-learn -> Spark ML -> TensorFlow pipeline)

![Image](docs/images/mllambda.png)

##### Story
Deploying skit-learn models for serving online user requests is pretty simple: just spin up an HTTP server, load the model and call `predict()` method. Performance wise it will satisfy most of the use cases.
TensorFlow serving is a bit more complicated to manage but feasible as well.
For Spark ML you would need to use [spark-ml-serving](https://github.com/Hydrospheredata/spark-ml-serving) library and call it from HTTP action.
JVM friendly H2O and DL4J would go the same way.

##### Challenge
In real projects you have to deploy pipelines of different models and custom functions rather that a single model.
You could not lock data scientist to use just 1 machine learning framework and build 1 HTTP server for that.
Data scientist needs to build->test->deploy to production quickly and continuously experimenting different models and ensembles.
Production data and predictions quality should be carefully monitored and the system should be adjusted in real time.

##### Example

To build a natural language processing pipeline that takes input JSON from online web application, extracts simple features, classifies a document into particular category and then applies a neural network pre-trained for detected category you’ll need something like this:

![Image](docs/images/NLP-serving-pipeline.png)

Then you've decided to try embedding word2vec into DL4J neural network as a first two layers. Your production experiment will look this way:

![Image](docs/images/serving-experiment.png) 

Then you've decided to execute the same serving pipline in a streaming context, i.e. deploy a prediction pipeline into Kafka. 

![Image](docs/images/serving-pipelines-in-kafka.png)

How would you do that as a data scientist? There is an option to ask engineers to re-implement it in Java/Spark that takes another 6 months and unpredictable outcome. There is another solution described below.

##### Solution
Hydro Serving manages multiple serverless runtimes and allows chaining them into end-to-end serving pipelines. Monitoring, versioning, auto-scaling, models auto discovery and user friendly UI goes on top.
It implements side-car architecture that decouples machine learning logic from networking, service discovery, load balancing. 
So, the transport layer between model runtimes could be changed from HTTP to unix sockets and even to Kafka without changing containers with actual machine learning code.

![Image](docs/images/Diagrams.png?raw=true)

Additional out of the box features include Rate limiting, Load balancing, Circuit breaking, Tracing, Statistics.

## Repository structure
* [manager](/manager) is a module that rules over all ML models, knows where they are, what they are
* [codegen](/codegen) is a module that handles code-generation (e.g. generate schema from db)
* [dummy-runtime](/dummy-runtime) is a module with runtime that is used in testing purposes
* [integrations](/integrations) is a folder with startup and env setup scripts

## Related repositories
 * Runtimes:
   * Tensorflow: https://github.com/Hydrospheredata/hydro-serving-tensorflow
   * Python: https://github.com/Hydrospheredata/hydro-serving-python
   * Spark: https://github.com/Hydrospheredata/hydro-serving-spark
     * Spark local inference implementation: https://github.com/Hydrospheredata/spark-ml-serving
 * Protobuf messages: https://github.com/Hydrospheredata/hydro-serving-protos
 * Sidecar implementation: https://github.com/Hydrospheredata/hydro-serving-sidecar
 * Example models: https://github.com/Hydrospheredata/hydro-serving-runtime/tree/master/models
    


## How to launch demo

### Build project
#### Clone repositories

```
#Sidecar + manager + gateway + dummy runtime 
git clone https://github.com/Hydrospheredata/hydro-serving

#ML Runtimes + ML models repository
git clone https://github.com/Hydrospheredata/hydro-serving-runtime
```

#### Build modules 
Change directory to `hydro-serving` and:
```
sbt compile docker
```

Change directory to `hydro-serving-runtime` and:
```
./build.sh
```

You will get next docker images:
* `hydrosphere/serving-manager` - ML model storage, it scans selected directory and parses founded ML models, also provides RestAPI to access this models
* `hydrosphere/serving-gateway` - Nginx gateway.
* `hydrosphere/serving-runtime-sparklocal` - Spark ML runtime, serves spark models
* `hydrosphere/serving-runtime-scikit` - Scikit runtime, serves scikit models.
* `hydrosphere/serving-runtime-tensorflow` - TF runtime.
* `hydrosphere/serving-runtime-py2databricks` - Python 2 runtime with Databricks-like environment.
* `hydrosphere/serving-runtime-python3` - Python 3 runtime.

#### Run
##### With Docker Compose
```
export MODEL_DIRECTORY=/path/to/hydro-serving-runtime/models
docker-compose up
```

##### Without Docker Compose
* Run Zipkin
```
docker run -p 9411:9411 openzipkin/zipkin:1.28.1
```
* Run database
```
docker run -e POSTGRES_DB=docker \
    -e POSTGRES_USER=docker \
    -e POSTGRES_PASSWORD=docker \
    -p 5432:5432 \
    postgres:9.6-alpine
```
* Run manager
```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
export MODEL_DIRECTORY=/path/to/hydro-serving-runtime/models
docker run -e ADVERTISED_MANAGER_HOST=$HOST_IP \
    -e DATABASE_HOST=$HOST_IP \
    -e ZIPKIN_ENABLED=true \
    -e ZIPKIN_HOST=$HOST_IP \
    -p 8080:8080 -p 8082:8082 -p 9090:9090 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v ${MODEL_DIRECTORY}:/models \
    hydrosphere/serving-manager:0.0.1
```
* Run gateway
```
HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -e MANAGER_HOST=$HOST_IP \
    -e ZIPKIN_ENABLED=true \
    -e ZIPKIN_HOST=$HOST_IP \
    -p 8180:8080 -p 8182:8082 -p 9190:9090 \
    hydrosphere/serving-gateway:0.0.1
```
### Available Resources
* jdbc:postgresql://localhost:5432/docker - Postgresql
* http://localhost:9411/zipkin - OpenTracing
* http://localhost:8080/swagger/swagger-ui.html - Manager
* http://localhost:8180/api/v1/serve/BLABLABLA - Gateway


### Create and run pipeline

#### Product matching pipeline
This pipeline gets description of two items, and compares them using following ML pipeline:
![Image](docs/images/pm_pipeline.png)

You can create it using next request:
```
curl -H "Content-Type: application/json" -X POST -d '{"name":"endpointML","transportType":"http", "chain":["serving-customscikit/pm_scikit","serving-localml-spark/pm_spark"]}' http://localhost:8080/api/v1/pipelines
```
Now, pipeline is created and you can see it in [Manager](http://localhost:8080/api/v1/pipelines).

And we will process this data
```
[
  {
    "Title1":"Apple iPhone 5 - 32GB - White & Silver (Verizon) Smartphone",
    "ItemSpecifics1":"'Brand': ' Apple'; 'Style': ' Bar'; 'Processor': ' Dual Core'; 'Camera Resolution': ' 8.0MP'; 'Color': ' Silver'; 'Lock Status': ' Network Locked'; 'Network': ' Verizon'; 'Model': ' iPhone 5'; 'Storage Capacity': ' 32GB'; 'Cosmetic condition': ' Excellent'; 'Screen Size': ' 4''; 'No accessories': ' Yes'; 'UPC': ' 492550400162'",
    "Title2":"Aquarium Fish Tank Vacuum Gravel Cleaner Water Siphon Pump Filter",
    "ItemSpecifics2":"'Brand': ' Unbranded'; 'EAN': ' 4894462250013'; 'MPN': ' Does not apply'; 'ISBN': ' 4894462250013'; 'UPC': ' 4894462250013'; 'Designer/Brand': ' Unbranded'"
  },
  {
    "Title1":"Apple iPhone 5 - 32GB - White & Silver (Verizon) Smartphone",
    "ItemSpecifics1":"'Brand': ' Apple'; 'Style': ' Bar'; 'Processor': ' Dual Core'; 'Camera Resolution': ' 8.0MP'; 'Color': ' Silver'; 'Lock Status': ' Network Locked'; 'Network': ' Verizon'; 'Model': ' iPhone 5'; 'Storage Capacity': ' 32GB'; 'Cosmetic condition': ' Excellent'; 'Screen Size': ' 4''; 'No accessories': ' Yes'; 'UPC': ' 492550400162'",
    "Title2":"Apple iPhone 5 - 32GB - White & Silver (Verizon) Smartphone",
    "ItemSpecifics2":"'Brand': ' Apple'; 'Style': ' Bar'; 'Processor': ' Dual Core'; 'Camera Resolution': ' 8.0MP'; 'Color': ' Silver'; 'Lock Status': ' Network Locked'; 'Network': ' Verizon'; 'Model': ' iPhone 5'; 'Storage Capacity': ' 32GB'; 'Cosmetic condition': ' Excellent'; 'Screen Size': ' 4''; 'No accessories': ' Yes'; 'UPC': ' 492550400162'"
  }
]
```

on the pipeline with following request
```
curl --request POST \
  --url http://localhost:8083/api/v1/serve/endpointML \
  --header 'content-type: application/json' \
  --data '[{"Title1":"Apple iPhone 5 - 32GB - White & Silver (Verizon) Smartphone","ItemSpecifics1":"'\''Brand'\'': '\'' Apple'\''; '\''Style'\'': '\'' Bar'\''; '\''Processor'\'': '\'' Dual Core'\''; '\''Camera Resolution'\'': '\'' 8.0MP'\''; '\''Color'\'': '\'' Silver'\''; '\''Lock Status'\'': '\'' Network Locked'\''; '\''Network'\'': '\'' Verizon'\''; '\''Model'\'': '\'' iPhone 5'\''; '\''Storage Capacity'\'': '\'' 32GB'\''; '\''Cosmetic condition'\'': '\'' Excellent'\''; '\''Screen Size'\'': '\'' 4'\'''\''; '\''No accessories'\'': '\'' Yes'\''; '\''UPC'\'': '\'' 492550400162'\''","Title2":"Aquarium Fish Tank Vacuum Gravel Cleaner Water Siphon Pump Filter","ItemSpecifics2":"'\''Brand'\'': '\'' Unbranded'\''; '\''EAN'\'': '\'' 4894462250013'\''; '\''MPN'\'': '\'' Does not apply'\''; '\''ISBN'\'': '\'' 4894462250013'\''; '\''UPC'\'': '\'' 4894462250013'\''; '\''Designer/Brand'\'': '\'' Unbranded'\''"},{"Title1":"Apple iPhone 5 - 32GB - White & Silver (Verizon) Smartphone","ItemSpecifics1":"'\''Brand'\'': '\'' Apple'\''; '\''Style'\'': '\'' Bar'\''; '\''Processor'\'': '\'' Dual Core'\''; '\''Camera Resolution'\'': '\'' 8.0MP'\''; '\''Color'\'': '\'' Silver'\''; '\''Lock Status'\'': '\'' Network Locked'\''; '\''Network'\'': '\'' Verizon'\''; '\''Model'\'': '\'' iPhone 5'\''; '\''Storage Capacity'\'': '\'' 32GB'\''; '\''Cosmetic condition'\'': '\'' Excellent'\''; '\''Screen Size'\'': '\'' 4'\'''\''; '\''No accessories'\'': '\'' Yes'\''; '\''UPC'\'': '\'' 492550400162'\''","Title2":"Apple iPhone 5 - 32GB - White & Silver (Verizon) Smartphone","ItemSpecifics2":"'\''Brand'\'': '\'' Apple'\''; '\''Style'\'': '\'' Bar'\''; '\''Processor'\'': '\'' Dual Core'\''; '\''Camera Resolution'\'': '\'' 8.0MP'\''; '\''Color'\'': '\'' Silver'\''; '\''Lock Status'\'': '\'' Network Locked'\''; '\''Network'\'': '\'' Verizon'\''; '\''Model'\'': '\'' iPhone 5'\''; '\''Storage Capacity'\'': '\'' 32GB'\''; '\''Cosmetic condition'\'': '\'' Excellent'\''; '\''Screen Size'\'': '\'' 4'\'''\''; '\''No accessories'\'': '\'' Yes'\''; '\''UPC'\'': '\'' 492550400162'\''"}]'
```
