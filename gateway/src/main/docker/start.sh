#!/bin/sh

SERVICE_ID=$1
JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

exec java $JAVA_OPTS -jar /hydro-serving/app/gateway.jar \
    --server.port=$APP_HTTP_PORT \
    --sideCar.host=localhost \
    --sideCar.port=$ENVOY_HTTP_PORT \
    --sideCar.serviceId=$SERVICE_ID \
    --gateway.managerHost=$MANAGER_HOST \
    --gateway.managerPort=$MANAGER_PORT