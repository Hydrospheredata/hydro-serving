#!/bin/sh

mkdir -p /var/log/hydro-serving/sidecar/

[ -z "$NETWORK_SUBNET" ] && NETWORK_SUBNET='0.0.0.0'

HOST_NODE_IP=$(netstat -nr | grep "^0\.0\.0\.0" | awk '{print $2}')
HOST_ETH=$(netstat -nr | grep "^0\.0\.0\.0" | awk '{print $8}')
HOST_CONTAINER_IP=$(ifconfig $HOST_ETH | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')

NODE_ETH=$(netstat -nr | grep "^$NETWORK_SUBNET" | awk '{print $8}')
CONTAINER_IP=$(ifconfig $NODE_ETH | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')
[ -z "$CONTAINER_ID" ] && CONTAINER_ID=$(cat /proc/self/cgroup | grep docker | grep -o -E '[0-9a-f]{64}' | head -n 1)

term_handler() {
  echo 'Stopping App....'
  if [ $APP_PID -ne 0 ]; then
    kill -s TERM "$APP_PID"
    wait "$APP_PID"
  fi

  echo 'Stopping Sidecar....'
  if [ $ENVOY_PID -ne 0 ]; then
    kill -s TERM "$ENVOY_PID"
    wait "$ENVOY_PID"
  fi
  echo 'App stopped.'
  exit
}

trap "term_handler" SIGHUP SIGINT SIGTERM

#Generate configuration for envoy
chmod +x /hydro-serving/sidecar/generate_envoy_config.sh
/hydro-serving/sidecar/generate_envoy_config.sh
hydro-serving/sidecar/envoy -c /hydro-serving/sidecar/envoy.json --service-cluster $SERVICE_TYPE --service-node $CONTAINER_ID &
ENVOY_PID=$!

chmod +x $APP_START_SCRIPT
$APP_START_SCRIPT $CONTAINER_ID &
APP_PID=$!

wait