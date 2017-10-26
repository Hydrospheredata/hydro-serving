FROM confluent/kafka
ENV JMX_PORT=1099
ENV KAFKA_JMX_PORT=1099
ENV KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false -javaagent:/etc/kafka/jmx_prometheus_javaagent.jar=1100:/jmx_exporter_config.yaml"
COPY jmx_exporter_config.yaml /
RUN curl http://central.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.10/jmx_prometheus_javaagent-0.10.jar --output /etc/kafka/jmx_prometheus_javaagent.jar
EXPOSE 9092 1099 1100
