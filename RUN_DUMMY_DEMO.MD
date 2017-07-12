### Start environment
```
docker swarm init
docker network create --driver overlay net1
docker stack deploy --compose-file docker-compose.yml hydroserving
```

### Available services
* http://localhost:9411/zipkin - OpenTracing
* http://localhost:8080/swagger-ui.html - Manager
* http://localhost:8081/swagger-ui.html - Gateway
* http://localhost:8082 - Repository

### Commands Example

* Run 3 models using: test1, test2, test3

```
curl -X POST --header 'Content-Type: application/json' --header 'Accept: */*' -d '{ \ 
   "appHttpPort": 9090, \ 
   "environments": { \ 
   }, \ 
   "httpPort": 8080, \ 
   "imageName": "hydro-serving/runtime-dummy:1.0-SNAPSHOT", \ 
   "name": "test1", \ 
   "scale": 1, \ 
 "runtimeType":"model", \ 
 "modelName":"dummy", \ 
 "modelType":"dummy", \ 
 "modelVersion":"dummy1" \ 
 }' 'http://localhost:8080/api/v1/runtime'
 
 curl -X POST --header 'Content-Type: application/json' --header 'Accept: */*' -d '{ \ 
    "appHttpPort": 9090, \ 
    "environments": { \ 
    }, \ 
    "httpPort": 8080, \ 
    "imageName": "hydro-serving/runtime-dummy:1.0-SNAPSHOT", \ 
    "name": "test2", \ 
    "scale": 1, \ 
  "runtimeType":"model", \ 
  "modelName":"dummy", \ 
  "modelType":"dummy", \ 
  "modelVersion":"dummy2" \ 
  }' 'http://localhost:8080/api/v1/runtime'
  
curl -X POST --header 'Content-Type: application/json' --header 'Accept: */*' -d '{ \ 
    "appHttpPort": 9090, \ 
    "environments": { \ 
    }, \ 
    "httpPort": 8080, \ 
    "imageName": "hydro-serving/runtime-dummy:1.0-SNAPSHOT", \ 
    "name": "test3", \ 
    "scale": 1, \ 
  "runtimeType":"model", \ 
  "modelName":"dummy", \ 
  "modelType":"dummy", \ 
  "modelVersion":"dummy3" \ 
  }' 'http://localhost:8080/api/v1/runtime'
```

* Fetch running models 

```
curl -X GET --header 'Accept: application/json' 'http://localhost:8080/api/v1/runtime'
```

* Fetch running instances
```
curl -X GET --header 'Accept: application/json' 'http://localhost:8080/api/v1/runtime/instances/test1'
```

* Create Pipeline
```
curl -X POST --header 'Content-Type: application/json' --header 'Accept: */*' -d '{ \ 
   "invocationChain": [ \ 
     "test1/serve", \ 
     "test2/serve", \ 
     "test3/serve" \ 
   ], \ 
   "name": "test" \ 
 }' 'http://localhost:8080/api/v1/pipelines'
```

* Invoke Pipeline
```
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '[{"someData":"Some"}]' 'http://localhost:8081/api/v1/serve/test'
```

* Trace your requests in Zipkin http://localhost:9411/zipkin

### Clean environment
```
docker stack rm hydroserving
docker service rm $(docker service ls -q)
docker network rm net1
docker swarm leave --force
```
