#!/usr/bin/env sh

SERVICE_ID=$1

[ -z "$JAVA_XMX" ] && JAVA_XMX="256M"

[ -z "$APP_HTTP_PORT" ] && APP_HTTP_PORT="9090"
[ -z "$SIDECAR_HTTP_PORT" ] && SIDECAR_HTTP_PORT="8080"

[ -z "$MANAGER_HOST" ] && MANAGER_HOST="localhost"
[ -z "$MANAGER_PORT" ] && MANAGER_PORT=$APP_HTTP_PORT


JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

APP_OPTS="-Dmanager.host=$MANAGER_HOST -Dmanager.port=$MANAGER_PORT"
APP_OPTS="$APP_OPTS -Dapplication.port=$APP_HTTP_PORT -Dapplication.appId=$SERVICE_ID"
APP_OPTS="$APP_OPTS -Dsidecar.port=$SIDECAR_HTTP_PORT"

if [ "$CUSTOM_CONFIG" = "" ]
then
   echo "Custom config does not exist"
else
   APP_OPTS="$APP_OPTS -Dconfig.file=$CUSTOM_CONFIG"
fi

echo "Running Gateway with:"
echo "JAVA_OPTS=$JAVA_OPTS"
echo "APP_OPTS=$APP_OPTS"

java $JAVA_OPTS $APP_OPTS -cp "/hydro-serving/app/gateway.jar:/hydro-serving/app/lib/*" io.hydrosphere.serving.gateway.GatewayBoot