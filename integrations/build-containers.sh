#!/bin/bash

docker build -t hydro-serving/kafka-with-prometheus kafka
docker build -t hydro-serving/prometheus prometheus