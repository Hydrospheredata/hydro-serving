## With Docker Compose
```
MODEL_DIRECTORY=/path/to/hydro-serving-runtime/models
docker-compose up
```

## Without Docker Compose
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
HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -e ADVERTISED_MANAGER_HOST=$HOST_IP \
    -e DATABASE_HOST=$HOST_IP \
    -e ZIPKIN_ENABLED=true \
    -e ZIPKIN_HOST=$HOST_IP \
    -p 8080:8080 -p 8082:8082 -p 9090:9090 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    hydro-serving/manager:0.0.1
```
* Run gateway
```
HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -e MANAGER_HOST=$HOST_IP \
    -e ZIPKIN_ENABLED=true \
    -e ZIPKIN_HOST=$HOST_IP \
    -p 8180:8080 -p 8182:8082 -p 9190:9090 \
    hydro-serving/gateway:0.0.1
```
## Available Resources
* jdbc:postgresql://localhost:5432/docker - Postgresql
* http://localhost:9411/zipkin - OpenTracing
* http://localhost:8080/swagger/swagger-ui.html - Manager
* http://localhost:8180/api/v1/serve/BLABLABLA - Gateway

## Sample Commands
* Create RuntimeType
```
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/runtimeType \
-d @- << EOF
{
"name": "dummy", 
"version": "0.0.1"
}
EOF
```
Result:
```
{
  "id": 1,
  "name": "dummy",
  "version": "0.0.1"
}
```

* Create Model Runtime
```
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:9090/api/v1/modelRuntime \
-d @- << EOF
{
 "imageName": "hydro-serving/dummy-runtime",
 "imageTag": "0.0.1",
 "imageMD5Tag": "f7ed57b80b3d",
 "modelName": "dummy",
 "modelVersion": "0.0.1",
 "source": "dummy",
 "runtimeTypeId": 1,
 "outputFields": [
   "dummy"
 ],
 "inputFields": [
   "dummy"
 ]
}
EOF
```
Result:
```
{
  "inputFields": [
    "dummy"
  ],
  "source": "dummy",
  "imageMD5Tag": "f7ed57b80b3d",
  "runtimeType": {
    "id": 1,
    "name": "dummy",
    "version": "0.0.1"
  },
  "imageName": "hydro-serving/dummy-runtime",
  "imageTag": "0.0.1",
  "modelName": "dummy",
  "id": 1,
  "modelVersion": "0.0.1",
  "outputFields": [
    "dummy"
  ],
  "created": "2017-07-31T07:46:29.885"
}
```

* Create Model Services
```
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/modelService \
-d @- << EOF
{
  "serviceName": "dummy1",
  "modelRuntimeId": 1
}
EOF

curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/modelService \
-d @- << EOF
{
  "serviceName": "dummy2",
  "modelRuntimeId": 1
}
EOF

curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/modelService \
-d @- << EOF
{
  "serviceName": "dummy3",
  "modelRuntimeId": 1
}
EOF
```

* Create Pipeline
```
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/pipelines \
-d @- << EOF
{
  "name": "dummy",
  "stages": [
    {
      "serviceId": 1,
      "servePath": "/servce"
    },
    {
      "serviceId": 2,
      "servePath": "/servce"
    },
    {
      "serviceId": 3,
      "servePath": "/servce"
    }
  ]
}
EOF
```
* Create Endpoint
```
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8080/api/v1/pipelines \
-d @- << EOF
{
  "name": "dummy",
  "currentPipelineId": 1
}
EOF
```

* Serve 
```
curl --header 'Content-Type: application/json' --header 'Accept: */*' \
-X POST http://localhost:8180/api/v1/serve/dummy \
-d @- << EOF
[{"test":"test"}]
EOF
```
Result:
```
[
    {
        "e26068348da4203c36f0d19906a4c35792e814042b3a43785b59051f52396966": 1501492379,
        "ba811763c5423f571434a723620961438d3ac67e304a1f5e464b65c170acdc12": 1501492379,
        "dd": "dd",
        "cd1771cb4171fa4a0c8282ce54a1731b2d6306ac8548c84e5365e958532c2429": 1501492379
    }
]
```