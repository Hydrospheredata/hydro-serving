#!/usr/bin/env sh

[ -z "$JAVA_XMX" ] && JAVA_XMX="256M"

#[ -z "$APP_HTTP_PORT" ] && APP_HTTP_PORT="9090"
#[ -z "$APP_PORT" ] && APP_PORT="9091"
[ -z "$APP_SHADOWING_ON" ] && APP_SHADOWING_ON="false"
[ -z "$SIDECAR_INGRESS_PORT" ] && SIDECAR_INGRESS_PORT="8080"
[ -z "$SIDECAR_EGRESS_PORT" ] && SIDECAR_EGRESS_PORT="8081"
[ -z "$SIDECAR_ADMIN_PORT" ] && SIDECAR_ADMIN_PORT="8082"
[ -z "$SIDECAR_HOST" ] && SIDECAR_HOST="localhost"

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

APP_OPTS="-Dapplication.grpcPort=9091 -Dapplication.port=9090 -Dapplication.shadowingOn=$APP_SHADOWING_ON -Dsidecar.adminPort=$SIDECAR_ADMIN_PORT -Dsidecar.ingressPort=$SIDECAR_INGRESS_PORT -Dsidecar.egressPort=$SIDECAR_EGRESS_PORT -Dsidecar.host=$SIDECAR_HOST"

if [ "$GELF_HOST" = "" ]
then
GELF_ENABLED="false"
else
GELF_ENABLED="true"
fi

if [ "$CUSTOM_CONFIG" = "" ]
then
    APP_OPTS="$APP_OPTS -DopenTracing.zipkin.enabled=$ZIPKIN_ENABLED -DopenTracing.zipkin.port=$ZIPKIN_PORT -DopenTracing.zipkin.host=$ZIPKIN_HOST"
    APP_OPTS="$APP_OPTS -Dmanager.advertisedHost=$ADVERTISED_MANAGER_HOST -Dmanager.advertisedPort=$ADVERTISED_MANAGER_PORT"
    APP_OPTS="$APP_OPTS -Ddatabase.jdbcUrl=jdbc:postgresql://$DATABASE_HOST:$DATABASE_PORT/$DATABASE_NAME"
    APP_OPTS="$APP_OPTS -Ddatabase.username=$DATABASE_USERNAME -Ddatabase.password=$DATABASE_PASSWORD"

    if [ "$CLOUD_DRIVER" = "swarm" ]; then
        APP_OPTS="$APP_OPTS -DcloudDriver.swarm.networkName=$NETWORK_NAME"
    elif [ "$CLOUD_DRIVER" = "ecs" ]; then
        APP_OPTS="$APP_OPTS -DcloudDriver.ecs.region=$ECS_DEPLOY_REGION"
        APP_OPTS="$APP_OPTS -DcloudDriver.ecs.cluster=$ECS_DEPLOY_CLUSTER"
        APP_OPTS="$APP_OPTS -DcloudDriver.ecs.accountId=$ECS_DEPLOY_ACCOUNT"
        APP_OPTS="$APP_OPTS -DdockerRepository.type=ecs"
        if [ "$GELF_ENABLED" = "true" ]; then
            APP_OPTS="$APP_OPTS -DcloudDriver.ecs.loggingGelfHost=$GELF_HOST"
        fi
    else
        if [ ! -z "$NETWORK_NAME" ]; then
            APP_OPTS="$APP_OPTS -DcloudDriver.docker.networkName=$NETWORK_NAME"
        else
            APP_OPTS="$APP_OPTS -DcloudDriver.docker.networkName=bridge"
        fi
        APP_OPTS="$APP_OPTS -DdockerRepository.type=local"
        if [ "$GELF_ENABLED" = "true" ]; then
            APP_OPTS="$APP_OPTS -DcloudDriver.docker.loggingGelfHost=$GELF_HOST"
        fi
    fi


    if [ -n "$LOCAL_MODEL_PATH" ]; then
        APP_OPTS="$APP_OPTS -DmodelSources.local.name=local"
        APP_OPTS="$APP_OPTS -DmodelSources.local.path=$LOCAL_MODEL_PATH"
    fi
    if [ -n "$S3_MODEL_PATH" ]; then
        APP_OPTS="$APP_OPTS -DmodelSources.s3.name=s3"
        APP_OPTS="$APP_OPTS -DmodelSources.s3.path=$S3_MODEL_PATH"
        APP_OPTS="$APP_OPTS -DmodelSources.s3.region=$S3_MODEL_REGION"
        APP_OPTS="$APP_OPTS -DmodelSources.s3.bucket=$S3_MODEL_BUCKET"
        APP_OPTS="$APP_OPTS -DmodelSources.s3.queue=$S3_MODEL_QUEUE"
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