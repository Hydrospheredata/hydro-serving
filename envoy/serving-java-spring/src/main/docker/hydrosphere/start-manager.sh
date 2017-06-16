#!/bin/sh

SERVICE_ID=$1
JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

exec java $JAVA_OPTS -jar /hydrosphere/serving-java-spring.jar \
    --server.port=$APP_HTTP_PORT \
    --sideCar.serviceGrpcPort=$APP_GRPC_PORT \
    --sideCar.host=localhost \
    --sideCar.httpPort=$ENVOY_HTTP_PORT \
    --sideCar.grpcPort=$ENVOY_GRPC_PORT \
    --serving.serviceName=$SERVICE_NAME \
    --sideCar.serviceId=$SERVICE_ID