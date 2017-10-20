#!/bin/bash

docker-compose stop
docker-compose rm -f
docker network rm automation_hydronet
