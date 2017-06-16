FROM lyft/envoy-alpine:${docker.envoy.version}
COPY hydrosphere /hydrosphere

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

RUN apk add --update curl \
    && chmod +x /hydrosphere/scripts/start.sh \
    && rm -rf /var/cache/apk/* /tmp/*

CMD ["/hydrosphere/scripts/start.sh"]