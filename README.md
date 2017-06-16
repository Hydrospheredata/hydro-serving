# Hydro-serving

This repository contains several sub-projects for serving ml models.

## Project proposal

1. By this project we accompish next goals:
  * Easy to serve complex models
  * High models reusability
  * Multi-framework pipelines (e.g. Scikit -> Spark pipeline)
  * Stability
2. How? By using microservice principles for ML model serving + REAL pipelines
![Image](docs/images/Diagrams.png?raw=true)
3. We chose envoy and sidecar because:
    * we move all "microservice" logic from ML Serving model to envoy
    * we can add any existing runtime to our cluster without code changes  
4. Additional features: rate limiting, Load balancing, Circuit breaking, Tracing, Statistics
5. Project roadmap:
    * move all pipline logic to envoy
    * create apk/deb
    * add GRPC stream interface to gateway

## Structure
* [envoy](/envoy) contains all envoy logic and base docker images.
* [mist-local-ml](/mist-local-ml) contains local SparkMl implementation. (Derived from Hydrosphere Mist)
* [mist-serving-gateway](/mist-serving-gateway) is a simple gateway of the whole project. For now, it's just set up Nginx.
* [ml_repository](/ml_repository) is a module that rules over all ML models, knows where they are, what they are.
* [ml_runtimes](/ml_runtimes) contains implementations for popular ML libraries. Runtime is a small server that can import user's model and provide an HTTP API to it.
* [models](/models) contains example ML models for implemented runtimes.


## How to launch demo

### Build project
0. Clone this repository
```
git clone https://github.com/provectus/hydro-serving.git
```

1. Build project using sh script:
```
./build.sh
```

You will get next docker images:
* `hydrosphere/pipelineserving-envoy-alpine` - common image with envoy-alpine.
* `hydrosphere/pipelineserving-java` - common image for all java applications.
* `hydrosphere/pipelineserving-python3` - common image for python3 applications.
* `hydrosphere/pipelineserving-gateway` - image with gateway app - will process all http requests from client.
* `hydrosphere/pipelineserving-manager` - image with manager app - manages all pipelines and envoys configurations.
* `hydrosphere/pipelineserving-serving-java-spring` - image with simple Spring Boot app.
* `mist-ml-repository` - ML model storage, it scans selected directory and parses founded ML models, also provides RestAPI to access this models
* `mist-runtime-sparklocal` - Spark ML runtime, serves spark models
* `mist-runtime-scikit` - Scikit runtime, serves scikit models.

2. Run infrastructure and manager:
```
docker-compose up consul zipkin manager
```
You will get:
* [Consul-UI](http://localhost:8500/ui/) - http://localhost:8500/ui/
* [Zipkin-UI](http://localhost:9411/) - http://localhost:9411/
* [Manager-RestAPI](http://localhost:8080/api/v1/pipelines) - http://localhost:8080/api/v1/pipelines

4. Run repository and gateway
```
# assuming you are in root of this repository
export MODEL_DIRECTORY=$(pwd)/models
docker-compose up gateway repository
```
You will get:
* [Gateway-RestAPI](http://localhost:8083/api/v1/serve/) - http://localhost:8083/api/v1/serve/...
* [Repository-RestAPI](http://localhost:8087) - http://localhost:8087
    1. `GET /metadata/<model_name>` returns metadata of specified model.
    2. `GET /files/<model_name>` returns a list of model's files.
    3. `GET /download/<model_name>/<file>` downloads the given file of specified model. Thus, repository also acts as a proxy between Runtime and actually the storage of models.


5. Run runtimes for demo
```
docker-compose up localml-spark custom-scikit
```

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
