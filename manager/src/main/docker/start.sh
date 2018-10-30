#!/usr/bin/env sh

[ -z "$JAVA_XMX" ] && JAVA_XMX="256M"

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
[ -z "$MAX_CONTENT_LENGTH" ] && MAX_CONTENT_LENGTH="1073741824"

JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

APP_OPTS="-Dapplication.grpc-port=9091 -Dapplication.port=9090"

if [ "$CUSTOM_CONFIG" = "" ]
then
    APP_OPTS="$APP_OPTS -Dakka.http.server.parsing.max-content-length=$MAX_CONTENT_LENGTH -Dakka.http.client.parsing.max-content-length=$MAX_CONTENT_LENGTH"
    APP_OPTS="$APP_OPTS -Dopen-tracing.zipkin.enabled=$ZIPKIN_ENABLED -Dopen-tracing.zipkin.port=$ZIPKIN_PORT -Dopen-tracing.zipkin.host=$ZIPKIN_HOST"
    APP_OPTS="$APP_OPTS -Dmanager.advertised-host=$ADVERTISED_MANAGER_HOST -Dmanager.advertised-port=$ADVERTISED_MANAGER_PORT"
    APP_OPTS="$APP_OPTS -Ddatabase.jdbc-url=jdbc:postgresql://$DATABASE_HOST:$DATABASE_PORT/$DATABASE_NAME"
    APP_OPTS="$APP_OPTS -Ddatabase.username=$DATABASE_USERNAME -Ddatabase.password=$DATABASE_PASSWORD"

    if [ "$CLOUD_DRIVER" = "swarm" ]; then
        APP_OPTS="$APP_OPTS -Dcloud-driver.type=swarm"
        APP_OPTS="$APP_OPTS -Dcloud-driver.networkName=$NETWORK_NAME"
    elif [ "$CLOUD_DRIVER" = "ecs" ]; then
        META_DATA_URL=http://169.254.169.254/latest
        SIDECAR_HOST=$(wget -q -O - $META_DATA_URL/meta-data/local-ipv4)
        LOCALITY_ZONE=$(wget -q -O - $META_DATA_URL/meta-data/placement/availability-zone)
        INTERFACE=$(wget -q -O - $META_DATA_URL/meta-data/network/interfaces/macs/)
        [ -z "$ECS_DEPLOY_REGION" ] && ECS_DEPLOY_REGION=$(echo $LOCALITY_ZONE | sed 's/[a-z]$//')
        [ -z "$ECS_DEPLOY_ACCOUNT" ] && ECS_DEPLOY_ACCOUNT=$(wget -q -O - $META_DATA_URL/dynamic/instance-identity/document| jq -r ".accountId")
        [ -z "$ECS_VPC_ID" ] && ECS_VPC_ID=$(wget -q -O - $META_DATA_URL/meta-data/network/interfaces/macs/${INTERFACE}/vpc-id)
        [ -z "$ECS_DEPLOY_MEMORY_RESERVATION" ] && ECS_DEPLOY_MEMORY_RESERVATION="200"

        APP_OPTS="$APP_OPTS -Dcloud-driver.type=ecs"
        APP_OPTS="$APP_OPTS -Ddocker-repository.type=ecs"
        APP_OPTS="$APP_OPTS -Dcloud-driver.internal-domain-name=$ECS_INTERNAL_DOMAIN_NAME"
        APP_OPTS="$APP_OPTS -Dcloud-driver.region=$ECS_DEPLOY_REGION"
        APP_OPTS="$APP_OPTS -Dcloud-driver.cluster=$ECS_DEPLOY_CLUSTER"
        APP_OPTS="$APP_OPTS -Dcloud-driver.account-id=$ECS_DEPLOY_ACCOUNT"
        APP_OPTS="$APP_OPTS -Dcloud-driver.vpcId=$ECS_VPC_ID"
        APP_OPTS="$APP_OPTS -Dcloud-driver.memory-reservation=$ECS_DEPLOY_MEMORY_RESERVATION"
    elif [ "$CLOUD_DRIVER" = "kubernetes" ]; then
        APP_OPTS="$APP_OPTS -Dcloud-driver.type=kubernetes"
        APP_OPTS="$APP_OPTS -Dcloud-driver.proxy-host=$KUBE_PROXY_SERVICE_HOST"
        APP_OPTS="$APP_OPTS -Dcloud-driver.proxy-port=$KUBE_PROXY_SERVICE_PORT"
        APP_OPTS="$APP_OPTS -Dcloud-driver.kube-namespace=$KUBE_NAMESPACE"
        APP_OPTS="$APP_OPTS -Dcloud-driver.kube-registry-secret-name=$KUBE_REGISTRY_SECRET_NAME"
        APP_OPTS="$APP_OPTS -Ddocker-repository.type=remote"
        APP_OPTS="$APP_OPTS -Ddocker-repository.host=$REMOTE_DOCKER_REGISTRY_HOST"
        [ ! -z "$REMOTE_DOCKER_REGISTRY_USERNAME" ] && APP_OPTS="$APP_OPTS -Ddocker-repository.username=$REMOTE_DOCKER_REGISTRY_USERNAME"
        [ ! -z "$REMOTE_DOCKER_REGISTRY_PASSWORD" ] && APP_OPTS="$APP_OPTS -Ddocker-repository.password=$REMOTE_DOCKER_REGISTRY_PASSWORD"
        [ ! -z "$REMOTE_DOCKER_PULL_HOST" ] && APP_OPTS="$APP_OPTS -Ddocker-repository.pull-host=$REMOTE_DOCKER_PULL_HOST"
        APP_OPTS="$APP_OPTS"
    else
        APP_OPTS="$APP_OPTS -Dcloud-driver.type=docker"
        if [ ! -z "$NETWORK_NAME" ]; then
            APP_OPTS="$APP_OPTS -Dcloud-driver.network-name=$NETWORK_NAME"
        else
            APP_OPTS="$APP_OPTS -Dcloud-driver.network-name=bridge"
        fi
        APP_OPTS="$APP_OPTS -Ddocker-repository.type=local"
    fi

    if [ -n "$METRICS_ELASTIC_URI" ]; then
        [ -z "$METRICS_ELASTIC_INDEX_NAME" ] && METRICS_ELASTIC_INDEX_NAME="metrics"
        [ -z "$METRICS_ELASTIC_MAPPING_NAME" ] && METRICS_ELASTIC_MAPPING_NAME="system"
        [ -z "$METRICS_ELASTIC_COLLECT_TIMEOUT" ] && METRICS_ELASTIC_COLLECT_TIMEOUT="30"

        APP_OPTS="$APP_OPTS -Dmetrics.elastic.collect-timeout=$METRICS_ELASTIC_COLLECT_TIMEOUT"
        APP_OPTS="$APP_OPTS -Dmetrics.elastic.client-uri=$METRICS_ELASTIC_URI"
        APP_OPTS="$APP_OPTS -Dmetrics.elastic.index-name=$METRICS_ELASTIC_INDEX_NAME"
        APP_OPTS="$APP_OPTS -Dmetrics.elastic.mapping-name=$METRICS_ELASTIC_MAPPING_NAME"
    fi

    if [ -n "$METRICS_INFLUXDB_HOST" ]; then
        [ -z "$METRICS_INFLUXDB_DATABASE_NAME" ] && METRICS_INFLUXDB_DATABASE_NAME="metrics"
        [ -z "$METRICS_INFLUXDB_PORT" ] && METRICS_INFLUXDB_PORT="8086"
        [ -z "$METRICS_INFLUXDB_COLLECT_TIMEOUT" ] && METRICS_INFLUXDB_COLLECT_TIMEOUT="30"

        APP_OPTS="$APP_OPTS -Dmetrics.influx-db.collect-timeout=$METRICS_INFLUXDB_COLLECT_TIMEOUT"
        APP_OPTS="$APP_OPTS -Dmetrics.influx-db.database-name=$METRICS_INFLUXDB_DATABASE_NAME"
        APP_OPTS="$APP_OPTS -Dmetrics.influx-db.host=$METRICS_INFLUXDB_HOST"
        APP_OPTS="$APP_OPTS -Dmetrics.influx-db.port=$METRICS_INFLUXDB_PORT"
    fi

    echo "Custom config does not exist"
else
   APP_OPTS="$APP_OPTS -Dconfig.file=$CUSTOM_CONFIG"
fi

APP_OPTS="$APP_OPTS -Dapplication.shadowing-on=$APP_SHADOWING_ON -Dsidecar.admin-port=$SIDECAR_ADMIN_PORT -Dsidecar.ingress-port=$SIDECAR_INGRESS_PORT -Dsidecar.egress-port=$SIDECAR_EGRESS_PORT -Dsidecar.host=$SIDECAR_HOST"

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
