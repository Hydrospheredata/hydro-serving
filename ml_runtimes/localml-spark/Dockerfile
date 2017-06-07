FROM hydrosphere/pipelineserving-java:1.0-SNAPSHOT

ENV MODEL_TYPE=type
ENV MODEL_NAME=name
ENV MODEL_VERSION=version
ENV SERVER_HOME=/app/
ENV SERVER_JAR=spark-localml-serve-assembly-1.0.jar
ENV SERVER_EXEC=${SERVER_HOME}/${SERVER_JAR}
ENV ORIGIN_JAR=target/scala-2.11/${SERVER_JAR}

ADD start-localml-spark.sh /hydrosphere/
ADD ${ORIGIN_JAR} ${SERVER_HOME}

ENV APP_START_SCRIPT=/hydrosphere/start-localml-spark.sh
ENV USE_APP_HTTP=true
ENV USE_APP_GRPC=false
ENV SERVICE_TYPE=serving
ENV SERVICE_NAME=localml-spark
ENV SERVICE_VERSION=0.0.1

ENV MANAGER_HOST=localhost
ENV MANAGER_PORT=8080

ENV ML_REPO_ADDR=localhost
ENV ML_REPO_PORT=8080