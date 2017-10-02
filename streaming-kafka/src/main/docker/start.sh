#!/usr/bin/env sh

SERVICE_ID=$1

[ -z "$JAVA_XMX" ] && JAVA_XMX="256M"

[ -z "$APP_HTTP_PORT" ] && APP_HTTP_PORT="9090"
[ -z "$SIDECAR_HTTP_PORT" ] && SIDECAR_HTTP_PORT="8080"

[ -z "$MANAGER_HOST" ] && MANAGER_HOST="localhost"
[ -z "$MANAGER_PORT" ] && MANAGER_PORT=$APP_HTTP_PORT

[ -z "$STREAMING_SOURCE_TOPIC" ] && STREAMING_SOURCE_TOPIC="source"
[ -z "$STREAMING_DESTINATION_TOPIC" ] && STREAMING_DESTINATION_TOPIC="destination"
[ -z "$STREAMING_PROCESSOR_ROUTE" ] && STREAMING_PROCESSOR_ROUTE="unknow"
[ -z "$STREAMING_BOOTSTRAP_SERVERS" ] && STREAMING_BOOTSTRAP_SERVERS="kafka:9092"
[ -z "$STREAMING_KAFKA_GROUP_ID" ] && STREAMING_KAFKA_GROUP_ID="someGroup"

JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

APP_OPTS="-Dmanager.host=$MANAGER_HOST -Dmanager.port=$MANAGER_PORT"
APP_OPTS="$APP_OPTS -Dapplication.port=$APP_HTTP_PORT -Dapplication.appId=$SERVICE_ID"
APP_OPTS="$APP_OPTS -Dsidecar.port=$SIDECAR_HTTP_PORT"

if [ "$CUSTOM_CONFIG" = "" ]
then
   echo "Custom config does not exist"
   APP_OPTS="$APP_OPTS -Dstreaming.sourceTopic=$STREAMING_SOURCE_TOPIC"
   APP_OPTS="$APP_OPTS -Dstreaming.destinationTopic=$STREAMING_DESTINATION_TOPIC"
   APP_OPTS="$APP_OPTS -Dstreaming.processorRoute=$STREAMING_PROCESSOR_ROUTE"

   APP_OPTS="$APP_OPTS -Dakka.kafka.consumer.kafka-clients.client.id=$SERVICE_ID"
   APP_OPTS="$APP_OPTS -Dakka.kafka.consumer.kafka-clients.group.id=$STREAMING_KAFKA_GROUP_ID"
   APP_OPTS="$APP_OPTS -Dakka.kafka.producer.kafka-clients.bootstrap.servers=$STREAMING_BOOTSTRAP_SERVERS"
   APP_OPTS="$APP_OPTS -Dakka.kafka.consumer.kafka-clients.bootstrap.servers=$STREAMING_BOOTSTRAP_SERVERS"
else
   APP_OPTS="$APP_OPTS -Dconfig.file=$CUSTOM_CONFIG"
fi

echo "Running streaming-kafka with:"
echo "JAVA_OPTS=$JAVA_OPTS"
echo "APP_OPTS=$APP_OPTS"

java $JAVA_OPTS $APP_OPTS -cp "/hydro-serving/app/streaming-kafka.jar:/hydro-serving/app/lib/*" io.hydrosphere.serving.streaming.StreamingKafkaBoot