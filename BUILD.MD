#### Description

Some text about:
1. Current problems in ML serving: complex models, models reusability, multi-framework pipelines, stability
2. How we solve them: using microservice principles for ML model serving + REAL pipelines
![Image](docs/images/Diagrams.png?raw=true)
3. Some text about envoy and sidecar. We have selected because:
    * we move all "microservice" logic from ML Serving model to envoy
    * we can add any existing runtime to our cluster without code changes  
4. We can: rate limiting, Load balancing, Circuit breaking, Tracing, Statistics
5. we will:
    * move all pipline logic to envoy
    * create apk/deb
    * add GRPC stream interface to gateway


### Build
0. Clone sources from repositories
```
#Envoy start scripts + envoy manager + gateway + simple runtime 
git clone https://github.com/Zajs/envoy-serving

#ML Runtimes + ML models repository
git clone -b dev_envoy https://github.com/provectus/hydro-serving.git
```

1. Build modules from first repository. Change directory to `envoy-serving` and:
```
mvn clean install
```
You will get next docker images:
* `hydrosphere/pipelineserving-envoy-alpine` - common image with envoy-alpine.
* `hydrosphere/pipelineserving-java` - common image for all java applications.
* `hydrosphere/pipelineserving-python3` - common image for python3 applications.
* `hydrosphere/pipelineserving-gateway` - image with gateway app - will process all http requests from client.
* `hydrosphere/pipelineserving-manager` - image with manager app - manages all pipelines and envoys configurations.
* `hydrosphere/pipelineserving-serving-java-spring` - image with simple Spring Boot app. 


2. Build images modules from second repository. Change directory to `hydro-serving` and:
```
./build.sh
```
You will get next docker images:
* `mist-ml-repository` - ML model storage, it scans selected directory and parses founded ML models, also provides RestAPI to access this models 
* `mist-runtime-sparklocal` - Spark ML runtime, serves spark models
* `mist-runtime-scikit` - Scikit runtime, serves scikit models. 

3. Run infrastructure and manager. Change directory to `envoy-serving` and: 
```
docker-compose up consul zipkin manager
```
You will get:
* [Consul-UI](http://localhost:8500/ui/) - http://localhost:8500/ui/
* [Zipkin-UI](http://localhost:9411/) - http://localhost:9411/
* [Manager-RestAPI](http://localhost:8080/api/v1/pipelines) - http://localhost:8080/api/v1/pipelines

4. Run repository and gateway
```
export MODEL_DIRECTORY=YOUR_PATH_TO_hydro-serving/models
docker-compose up gateway repository
```
You will get:
* [Gateway-RestAPI](http://localhost:8083/api/v1/serve/) - http://localhost:8083/api/v1/serve/...
* [Repository-RestAPI](http://localhost:8087) - http://localhost:8087
    1. `GET /metadata/<model_name>` returns metadata of specified model.
    2. `GET /files/<model_name>` returns a list of model's files.
    3. `GET /download/<model_name>/<file>` downloads the given file of specified model. Thus, repository also acts as a proxy between Runtime and actually the storage of models.


5. Run runtime
```
docker-compose up localml-spark mist-runtime-scikit model1 model2 model3 model4
```

### Pipeline Examples

#### GRPC async pipeline
```
curl -H "Content-Type: application/json" -X POST -d '{"name":"endpoint1", "chain":["serving-model1","serving-model2","serving-model3","serving-model4"]}' http://localhost:8080/api/v1/pipelines
```
This will create next pipeline
![Image](docs/images/4modelpipeline.png?raw=true)

```
curl -H "Content-Type: application/json" -X POST -d '[{"test":"test"}]' http://localhost:8083/api/v1/serve/endpoint1
```

#### HTTP pipeline

```
curl -H "Content-Type: application/json" -X POST -d '{"name":"endpointHttp","transportType":"http", "chain":["serving-model1/v1/serving","serving-model2/v1/serving","serving-model3/v1/serving","serving-model4/v1/serving"]}' http://localhost:8080/api/v1/pipelines
```
This will create next pipeline
![Image](docs/images/4http.png?raw=true)

```
curl -H "Content-Type: application/json" -X POST -d '[{"test":"test"}]' http://localhost:8083/api/v1/serve/endpointHttp
```

#### Spark + Scikit learn

```
curl -H "Content-Type: application/json" -X POST -d '{"name":"endpointML","transportType":"http", "chain":["serving-model1/v1/serving","serving-scikit/svm","serving-localml-spark/dtreeregressor","serving-model2/v1/serving"]}' http://localhost:8080/api/v1/pipelines
```
This will create next pipeline
![Image](docs/images/realml.png?raw=true)

```
curl --request POST \
  --url http://localhost:8083/api/v1/serve/endpointML \
  --header 'content-type: application/json' \
--data '[{"sepal length (cm)":5.0,"sepal width (cm)":3.0,"petal length (cm)":1.6,"petal width (cm)":0.2,"features":[1,2,3,4,5]},{"sepal length (cm)":5.9,"sepal width (cm)":3.0,"petal length (cm)":5.1,"petal width (cm)":1.8,"features":[8,0,5,1,7]}]'
```