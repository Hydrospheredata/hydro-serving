FROM hydrosphere/pipelineserving-java:1.0-SNAPSHOT

ENV SERVER_HOME=/app/
ENV SERVER_JAR=ml_repository-assembly-1.0.jar

ENV ORIGIN_JAR=target/scala-2.11/${SERVER_JAR}
ENV SERVER_EXEC=${SERVER_HOME}/${SERVER_JAR}

ENV APP_START_SCRIPT=/hydrosphere/start-repo.sh
ENV USE_APP_HTTP=true
ENV USE_APP_GRPC=false
ENV SERVICE_TYPE=repository
ENV SERVICE_NAME=repository
ENV SERVICE_VERSION=0.0.1

ENV MANAGER_HOST=localhost
ENV MANAGER_PORT=8080

ADD start-repo.sh /hydrosphere/
ADD config ${SERVER_HOME}/config
ADD ${ORIGIN_JAR} ${SERVER_HOME}