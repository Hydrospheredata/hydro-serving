### Install in docker

./hydro-serving-sidecar-1.0-SNAPSHOT.sh --target /usr/share/hydro-serving/ -- alpine

```
FROM ${your-image}
COPY hydro-serving-sidecar-1.0-SNAPSHOT.sh /

ENV ENVOY_HTTP_PORT=8080
ENV ENVOY_GRPC_PORT=8081
ENV ENVOY_ADMIN_PORT=8082

ENV APP_HTTP_PORT=9090
ENV APP_GRPC_PORT=9091

ENV USE_APP_HTTP=false
ENV USE_APP_GRPC=true
ENV APP_START_SCRIPT=/hydrosphere/scripts/dummy.sh

ENV SERVICE_TYPE=dummy
ENV SERVICE_NAME=dummy
ENV SERVICE_VERSION=dummy

ENV MANAGER_HOST=localhost
ENV MANAGER_PORT=8080

ENV ZIPKIN_ENABLED=false
ENV ZIPKIN_HOST=localhost
ENV ZIPKIN_PORT=9411

RUN ./hydro-serving-sidecar-1.0-SNAPSHOT.sh --target /usr/share/hydro-serving/ -- alpine \
    && rm -f hydro-serving-sidecar-1.0-SNAPSHOT.sh

CMD ["/usr/share/hydro-serving/start.sh"]
```