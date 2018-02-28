#!/bin/bash

./build-containers.sh

docker-compose up -f automation/demo/docker-compose.yml