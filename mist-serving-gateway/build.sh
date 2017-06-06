#!/usr/bin/env bash

docker build --no-cache -t mist-serving-gateway ./mist-serving-gateway
docker build --no-cache -t simple-mist-runtime ./simple-mist-runtime