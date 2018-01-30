```
docker run -e POSTGRES_DB=docker \
    -e POSTGRES_USER=docker \
    -e POSTGRES_PASSWORD=docker \
    -p 5432:5432 \
    postgres:9.6-alpine
```


```
export HOST_IP=$(ifconfig en0 | grep 'inet ' |  awk '{ print $2}')
docker run -e MANAGER_HOST=$HOST_IP \
    -e MANAGER_PORT=9091 \
    -e SERVICE_ID=-20 \
    -e SERVICE_NAME="manager" \
    -p 8080:8080 -p 8081:8081 -p 8082:8082 \
    hydro-serving-sidecar
```

```
-DmodelSources.local.path=/Users/eduarddautov/work/provectus/hydro-serving-runtime/models
-Dsidecar.host=$HOST_IP
-Dmanager.advertisedHost=$HOST_IP
-Dmanager.advertisedPort=9091
```