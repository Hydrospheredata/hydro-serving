FROM hydrosphere/pipelineserving-java:${docker.current.build.version}

ENV USE_APP_HTTP=true
ENV USE_APP_GRPC=true
ENV APP_START_SCRIPT=/hydrosphere/start-manager.sh
ENV SERVICE_TYPE=gateway
ENV SERVICE_NAME=gateway
ENV SERVICE_VERSION=${docker.current.build.version}

ENV MANAGER_HOST=localhost
ENV MANAGER_PORT=8080

ENV JAVA_XMX=256M

COPY hydrosphere hydrosphere
COPY maven /