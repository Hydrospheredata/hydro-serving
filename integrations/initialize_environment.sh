#!/bin/bash

COMPOSE_FILE=$1
[ -z "$COMPOSE_FILE" ] && COMPOSE_FILE="docker-compose.yml"

docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic test_weight
docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic success_weight
docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic failure_weight

docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic test
docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic success
docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic failure
docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic shadow_topic

docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic entry-topic
docker-compose -f $COMPOSE_FILE exec kafka kafka-topics --create --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181 --topic metrics-topic

GF_SECURITY_ADMIN_PASSWORD=$(grep GF_SECURITY_ADMIN_PASSWORD $COMPOSE_FILE | cut -d = -f 2 | cut -d '"' -f 1)

curl http://admin:${GF_SECURITY_ADMIN_PASSWORD}@localhost:3000/api/datasources -X POST -H 'Content-Type: application/json;charset=UTF-8' \
   --data-binary '{"Name":"Prometheus","Type":"prometheus","url":"http://prometheus:9090","Access":"proxy","isDefault":true}'

curl http://admin:${GF_SECURITY_ADMIN_PASSWORD}@localhost:3000/api/datasources -X POST -H 'Content-Type: application/json;charset=UTF-8' \
   --data-binary '{"Name":"InfluxDB","Type":"influxdb", "database":"metrics", "url":"http://influxdb:8086","Access":"proxy","isDefault":false}'

curl http://admin:${GF_SECURITY_ADMIN_PASSWORD}@localhost:3000/api/datasources -X POST -H 'Content-Type: application/json;charset=UTF-8' \
   --data-binary '{"Name":"InfluxDBAppMetrics","Type":"influxdb", "database":"appmetrics", "url":"http://influxdb:8086","Access":"proxy","isDefault":false}'

for i in $(ls grafana | grep json); do
   curl http://admin:${GF_SECURITY_ADMIN_PASSWORD}@localhost:3000/api/dashboards/db -X POST -H 'Content-Type: application/json;charset=UTF-8' --data-binary "@./grafana/$i"
done


runtimeId=$(curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "name": "hydrosphere/serving-runtime-spark",
   "version": "2.1-latest",
   "modelTypes": [
     "spark:2.1"
   ],
   "tags": [
     "string"
   ],
   "configParams": {}
 }' 'http://localhost:8080/api/v1/runtime' | jq '.id')

curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "name": "hydrosphere/serving-runtime-spark",
   "version": "2.2-latest",
   "modelTypes": [
     "spark:2.2"
   ],
   "tags": [
     "string"
   ],
   "configParams": {}
 }' 'http://localhost:8080/api/v1/runtime'

curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
    "name": "hydrosphere/serving-runtime-spark",
    "version": "2.0-latest",
    "modelTypes": [
      "spark:2.0"
    ],
    "tags": [
      "string"
    ],
    "configParams": {}
  }' 'http://localhost:8080/api/v1/runtime'
