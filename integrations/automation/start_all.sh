#!/bin/bash

if [ ! -d hydro-serving-runtime ]; then
  git clone git@github.com:Hydrospheredata/hydro-serving-runtime.git
fi

if [ ! -d docker_monitoring_logging_alerting ]; then
  git clone git@github.com:uschtwill/docker_monitoring_logging_alerting.git
fi

docker network create --subnet=172.16.0.0/24 --internal automation_hydronet

docker-compose  up -d

sleep 10

pushd grafana
./add_data.sh
popd


