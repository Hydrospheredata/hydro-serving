FROM anapsix/alpine-java:8

ENV MODEL_TYPE=type
ENV MODEL_NAME=name
ENV MODEL_VERSION=version
ENV SERVER_HOME=/app/
ENV SERVER_JAR=spark-localml-serve-assembly-1.0.jar
ENV SERVER_EXEC=${SERVER_HOME}/${SERVER_JAR}
ENV ORIGIN_JAR=target/scala-2.11/${SERVER_JAR}

ADD consul.json ${SERVER_HOME}
ADD start.sh ${SERVER_HOME}
ADD ${ORIGIN_JAR} ${SERVER_HOME}

RUN apk add --update curl && rm -rf /var/cache/apk/* /tmp/*

WORKDIR ${SERVER_HOME}

EXPOSE 8080

CMD "./start.sh"