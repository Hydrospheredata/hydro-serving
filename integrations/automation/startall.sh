#!/bin/bash

if [ ! -d hydro-serving-runtime ]; then
  git clone git@github.com:Hydrospheredata/hydro-serving-runtime.git
fi

if [ ! -d docker_monitoring_logging_alerting ]; then
  git clone git@github.com:uschtwill/docker_monitoring_logging_alerting.git
fi

docker-compose  up -d
