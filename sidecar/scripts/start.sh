#!/bin/sh

mkdir -p /var/log/hydro-serving/sidecar/

[ -z "$CONTAINER_ID" ] && CONTAINER_ID=$(cat /proc/self/cgroup | grep docker | grep -o -E '[0-9a-f]{64}' | head -n 1)
[ -z "$HS_SERVICE_ID" ] && HS_SERVICE_ID="-1"

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
/hydro-serving/sidecar/generate_envoy_config.sh
/hydro-serving/sidecar/envoy -c /hydro-serving/sidecar/envoy.json --service-cluster $HS_SERVICE_ID --service-node $CONTAINER_ID &
ENVOY_PID=$!

chmod +x $APP_START_SCRIPT
sync
$APP_START_SCRIPT $CONTAINER_ID &
APP_PID=$!

wait