#!/usr/bin/env sh

SERVICE_ID=$1

[ -z "$JAVA_XMX" ] && JAVA_XMX="256M"

[ -z "$APP_HTTP_PORT" ] && APP_HTTP_PORT="9090"
[ -z "$SIDECAR_HTTP_PORT" ] && SIDECAR_HTTP_PORT="8080"

[ -z "$ADVERTISED_MANAGER_HOST" ] && ADVERTISED_MANAGER_HOST="manager"
[ -z "$ADVERTISED_MANAGER_PORT" ] && ADVERTISED_MANAGER_PORT="8080"

[ -z "$ZIPKIN_ENABLED" ] && ZIPKIN_ENABLED="false"
[ -z "$ZIPKIN_HOST" ] && ZIPKIN_HOST="zipkin"
[ -z "$ZIPKIN_PORT" ] && ZIPKIN_PORT="9411"

[ -z "$DATABASE_HOST" ] && DATABASE_HOST="postgresql"
[ -z "$DATABASE_PORT" ] && DATABASE_PORT="5432"
[ -z "$DATABASE_NAME" ] && DATABASE_NAME="docker"
[ -z "$DATABASE_USERNAME" ] && DATABASE_USERNAME="docker"
[ -z "$DATABASE_PASSWORD" ] && DATABASE_PASSWORD="docker"

JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

APP_OPTS="-Dapplication.port=$APP_HTTP_PORT -Dapplication.appId=$SERVICE_ID -Dsidecar.port=$SIDECAR_HTTP_PORT"

if [ "$CUSTOM_CONFIG" = "" ]
then
    APP_OPTS="$APP_OPTS -DopenTracing.zipkin.enabled=$ZIPKIN_ENABLED -DopenTracing.zipkin.port=$ZIPKIN_PORT -DopenTracing.zipkin.host=$ZIPKIN_HOST"
    APP_OPTS="$APP_OPTS -Dmanager.advertisedHost=$ADVERTISED_MANAGER_HOST -Dmanager.advertisedPort=$ADVERTISED_MANAGER_PORT"
    APP_OPTS="$APP_OPTS -Ddatabase.jdbcUrl=jdbc:postgresql://$DATABASE_HOST:$DATABASE_PORT/$DATABASE_NAME"
    APP_OPTS="$APP_OPTS -Ddatabase.username=$DATABASE_USERNAME -Ddatabase.password=$DATABASE_PASSWORD"
    if [ -n "$SWARM_NETWORK_NAME" ]; then
        APP_OPTS="$APP_OPTS -DcloudDriver.swarm.networkName=$SWARM_NETWORK_NAME"
    fi
    if [ -n "$DOCKER_NETWORK_NAME" ]; then
        APP_OPTS="$APP_OPTS -DcloudDriver.docker.networkName=$DOCKER_NETWORK_NAME"
    fi
    echo "Custom config does not exist"
else
   APP_OPTS="$APP_OPTS -Dconfig.file=$CUSTOM_CONFIG"
fi

echo "Running Manager with:"
echo "JAVA_OPTS=$JAVA_OPTS"
echo "APP_OPTS=$APP_OPTS"

#TODO Dirty hack, because of jffi-1.2.9-native.jar
mkdir -p /tmp /usr/lib64/
mkdir -p /usr/lib64/
unzip /hydro-serving/app/lib/jffi-*-native.jar jni/x86_64-Linux/* -d /tmp
cp -r /tmp/jni/x86_64-Linux/* /usr/lib64/
rm -rf /tmp/jni

java $JAVA_OPTS $APP_OPTS -cp "/hydro-serving/app/manager.jar:/hydro-serving/app/lib/*" io.hydrosphere.serving.manager.ManagerBoot