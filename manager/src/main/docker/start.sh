#!/bin/sh

SERVICE_ID=$1
JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

exec java $JAVA_OPTS -jar /hydro-serving/app/manager.jar \
    --server.port=$APP_HTTP_PORT \
    --sideCar.host=localhost \
    --sideCar.port=$ENVOY_HTTP_PORT \
    --sideCar.serviceId=$SERVICE_ID \
    --manager.exposedHost=$MANAGER_EXPOSED_HOST \
    --manager.exposedPort=$MANAGER_EXPOSED_PORT \
    --clouddriver.swarm.network=$SWARM_NETWORK_NAME \
    --manager.managerServiceName=$MANAGER_SERVICE_NAME \
    --manager.gatewayServiceName=$GATEWAY_SERVICE_NAME \
    --manager.repositoryServiceName=$REPOSITORY_SERVICE_NAME