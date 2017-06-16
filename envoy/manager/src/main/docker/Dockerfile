FROM hydrosphere/pipelineserving-java:${docker.current.build.version}

ENV CONSUL_HOST=localhost
ENV CONSUL_PORT=8500

ENV USE_APP_HTTP=true
ENV USE_APP_GRPC=false
ENV APP_START_SCRIPT=/hydrosphere/start-manager.sh
ENV SERVICE_TYPE=manager
ENV SERVICE_NAME=global-cluster-manager
ENV SERVICE_VERSION=${docker.current.build.version}

ENV MANAGER_HOST=localhost
ENV MANAGER_PORT=${APP_HTTP_PORT}
ENV MANAGER_EXTERNAL=192.168.90.112

ENV JAVA_XMX=256M

COPY hydrosphere hydrosphere
COPY maven /