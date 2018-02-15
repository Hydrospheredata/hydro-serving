## Development environment

![Image](HydroServingDeployemnt_dev.png)

TBD

#### How to start

1. Build `manager` and `dummy-runtime`.
```
sbt compile docker
```

##### Run PostgreSQL.
```
docker run -e POSTGRES_DB=docker \
    -e POSTGRES_USER=docker \
    -e POSTGRES_PASSWORD=docker \
    -p 5432:5432 \
    postgres:9.6-alpine
```

##### Run `sidecar`.
It will automatically connect to `manager` when it starts.

```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -e MANAGER_HOST=$HOST_IP \
    -e MANAGER_PORT=9091 \
    -e SERVICE_ID=-20 \
    -e SERVICE_NAME="manager" \
    -p 8080:8080 -p 8081:8081 -p 8082:8082 \
    hydrosphere/serving-sidecar:latest
```

##### Run `manager`.
###### Using IntelliJ IDEA.   
Specify `VM Options` in `Run/Debug Configurations` for `io.hydrosphere.serving.manager.ManagerBoot` application:
```
-DmodelSources.local.path=/path/to/models 
-Dsidecar.host=YOUR_HOST_API 
-Dmanager.advertisedHost=YOUR_HOST_API 
-Dmanager.advertisedPort=9091
```

###### Using sbt.
```
TBD
```

##### Run `manager-ui` (optional).
```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -p 8083:80 -e MANAGER_HOST=$HOST_IP:8080 hydrosphere/serving-manager-ui:latest
```

#### Resources
 - http://localhost:8080/swagger/swagger-ui.html - Swagger UI through Sidecar
 - http://localhost:9090/swagger/swagger-ui.html - Direct access to Swagger UI through Manager Http Port
 - http://localhost:8082 - Sidecar admin console
 - http://localhost:8083 - Manager UI
 - localhost:8080 - Sidecar Ingress Port HTTP/GRPC requests
 - localhost:8081 - Sidecar Egress Port HTTP/GRPC requests
 - localhost:9090 - Manager Http Port
 - localhost:9091 - Manager Grpc Port
 - localhost:5432 - PostgreSQL Port