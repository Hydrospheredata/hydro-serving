### Start environment
```
docker swarm init
docker network create --driver overlay net1
export MODEL_DIRECTORY=$(pwd)/../hydro-serving-runtime/models
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
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/runtime \
-d @- << EOF
{
  "appHttpPort": 9090, 
  "environments": { 
  },
  "httpPort": 8080,
  "imageName": "hydro-serving/runtime-dummy:1.0-SNAPSHOT", 
  "name": "test1",
  "scale": 1,
  "runtimeType":"model",
  "modelName":"dummy",  
  "modelType":"dummy", 
  "modelVersion":"dummy1" 
}
EOF

curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/runtime \
-d @- << EOF
{
  "appHttpPort": 9090, 
  "environments": { 
  },
  "httpPort": 8080,
  "imageName": "hydro-serving/runtime-dummy:1.0-SNAPSHOT", 
  "name": "test2",
  "scale": 1,
  "runtimeType":"model",
  "modelName":"dummy",  
  "modelType":"dummy", 
  "modelVersion":"dummy2" 
}
EOF

curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/runtime \
-d @- << EOF
{
  "appHttpPort": 9090, 
  "environments": { 
  },
  "httpPort": 8080,
  "imageName": "hydro-serving/runtime-dummy:1.0-SNAPSHOT", 
  "name": "test3",
  "scale": 1,
  "runtimeType":"model",
  "modelName":"dummy",  
  "modelType":"dummy", 
  "modelVersion":"dummy3" 
}
EOF
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
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/pipelines \
-d @- << EOF
{
   "invocationChain": [ 
     "test1/serve", 
     "test2/serve",  
     "test3/serve" 
   ],
   "name": "test" 
 }
EOF
```

* Invoke Pipeline
```
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8081/api/v1/serve/test \
-d @- << EOF
[{"someData":"Some"}]
EOF
```

* Trace your requests in Zipkin http://localhost:9411/zipkin

### Clean environment
```
docker stack rm hydroserving
docker service rm $(docker service ls -q)
docker network rm net1
docker swarm leave --force
```


### Without compose
HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -e POSTGRES_DB=docker -e POSTGRES_USER=docker -e POSTGRES_PASSWORD=docker -p 5432:5432 postgres:9.6-alpine
docker run -e ADVERTISED_MANAGER_HOST=$HOST_IP -e DATABASE_HOST=$HOST_IP -p 8080:8080 -p 8082:8082 -p 9090:9090 -v /var/run/docker.sock:/var/run/docker.sock hydro-serving/manager:0.0.1
docker run -e MANAGER_HOST=$HOST_IP -e MANAGER_PORT=9090 -p 8180:8080 -p 8182:8082 -p 9190:9090 hydro-serving/gateway:0.0.1

curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:9090/api/v1/runtimeType \
-d @- << EOF
{
"id": 0, 
"name": "dummy", 
"version": "0.0.1"
}
EOF
 
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:9090/api/v1/modelRuntime \
-d @- << EOF
{
 "id": 0,
 "imageName": "hydro-serving/dummy-runtime",
 "imageTag": "0.0.1",
 "imageMD5Tag": "2dad1a32d755",
 "modelName": "dummy",
 "modelVersion": "dummy",
 "source": "dummy",
 "runtimeType": {
   "id": 1,
   "name": "dummy",
   "version": "dummy"
 },
 "outputFields": [
   "dummy"
 ],
 "inputFields": [
   "dummy"
 ],
 "created": "2017-07-30T12:53:12.201Z"
}
EOF