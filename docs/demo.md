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
