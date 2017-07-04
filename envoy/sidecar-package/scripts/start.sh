#!/bin/sh

mkdir -p /var/log/envoy
mkdir -p /hydrosphere/configs

[ -z "$NETWORK_SUBNET" ] && NETWORK_SUBNET='0.0.0.0'

HOST_NODE_IP=$(netstat -nr | grep "^0\.0\.0\.0" | awk '{print $2}')
HOST_ETH=$(netstat -nr | grep "^0\.0\.0\.0" | awk '{print $8}')
HOST_CONTAINER_IP=$(ifconfig $HOST_ETH | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')

NODE_ETH=$(netstat -nr | grep "^$NETWORK_SUBNET" | awk '{print $8}')
CONTAINER_IP=$(ifconfig $NODE_ETH | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')
[ -z "$CONTAINER_ID" ] && CONTAINER_ID=$(cat /proc/self/cgroup | grep docker | grep -o -E '[0-9a-f]{64}' | head -n 1)

term_handler() {
  #deregistering service
  curl --retry 3  -v -X DELETE http://$MANAGER_HOST:$MANAGER_PORT/v1/registration/$CONTAINER_ID
  echo 'Stopping App....'
  if [ $APP_PID -ne 0 ]; then
    kill -s TERM "$APP_PID"
    wait "$APP_PID"

  fi
  echo 'Stopping Envoy....'
  if [ $ENVOY_PID -ne 0 ]; then
    kill -s TERM "$ENVOY_PID"
    wait "$ENVOY_PID"

  fi
  echo 'App stopped.'
  exit
}

trap "term_handler" SIGHUP SIGINT SIGTERM

#Generate configuration for envoy
chmod +x /hydrosphere/scripts/generate_envoy_config.sh
/hydrosphere/scripts/generate_envoy_config.sh
/usr/local/bin/envoy -c /hydrosphere/configs/envoy.json --service-cluster $SERVICE_TYPE --service-node $CONTAINER_ID &
ENVOY_PID=$!

chmod +x $APP_START_SCRIPT
$APP_START_SCRIPT $CONTAINER_ID &
APP_PID=$!

if [ "$USE_APP_GRPC" == "true" ]; then
#wait server start
while ! nc -z localhost $APP_GRPC_PORT; do
  sleep 0.1 # wait for 1/10 of the second before check again
done
fi

if [ "$USE_APP_HTTP" == "true" ]; then
#wait server start
while ! nc -z localhost $APP_HTTP_PORT; do
  sleep 0.1 # wait for 1/10 of the second before check again
done
fi

#generating json for service discovery
cat <<EOF > /hydrosphere/configs/registration.json
{
  "hostIp":"$HOST_NODE_IP",
  "ip":"$CONTAINER_IP",
  "sideCarAdminPort":"$ENVOY_ADMIN_PORT",
  "sideCarGrpcPort":"$ENVOY_GRPC_PORT",
  "sideCarHttpPort":"$ENVOY_HTTP_PORT",
  "serviceHttpPort":"$APP_HTTP_PORT",
  "serviceGrpcPort":"$APP_GRPC_PORT",
  "useServiceGrpc":"$USE_APP_GRPC",
  "useServiceHttp":"$USE_APP_HTTP",
  "serviceName":"$SERVICE_NAME",
  "serviceVersion":"$SERVICE_VERSION",
  "serviceId":"$CONTAINER_ID",
  "serviceType":"$SERVICE_TYPE"
}
EOF
#registering in service discovery
curl --retry 3 -v -X PUT -H "Content-Type: application/json" --data @/hydrosphere/configs/registration.json http://$MANAGER_HOST:$MANAGER_PORT/v1/registration
exit_status=$?
if [ $exit_status != 0 ]; then
    echo "Can't register service in SD"
    exit $exit_status
fi

wait