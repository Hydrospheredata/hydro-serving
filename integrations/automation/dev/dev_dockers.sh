#!/bin/bash

function containerId() {
  NAME=$1
  docker ps -a | grep $NAME | awk '{print $1}' | head -n 1
}

function start() {
  OPTS=$1
  PG_ID=$(containerId "postgres:9.6-alpine")
  if [ ! -z $PG_ID ]; then
      docker start $PG_ID
  else
      docker run -d -e POSTGRES_DB=docker -e POSTGRES_USER=docker -e POSTGRES_PASSWORD=docker -p 5432:5432 postgres:9.6-alpine
  fi

  ZIPKIN_ID=$(containerId "openzipkin/zipkin")
  if [ ! -z $ZIPKIN_ID ]; then
      docker start $ZIPKIN_ID
  else
      docker run -d -p 9411:9411 openzipkin/zipkin
  fi
  
  if [ "x$OPTS" == "xall" ]; then
    KAFKA_ID=$(containerId "spotify/kafka")
    if [ ! -z $KAFKA_ID ]; then
        docker start $KAFKA_ID
    else
        HOST_IP=$(route | grep default | awk '{print $8}' | head -n 1 | xargs ifconfig | grep inet | head -n 1 | awk '{print $2}')
        docker run -d -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=$HOST_IP --env ADVERTISED_PORT=9092 spotify/kafka 
    fi
  fi

}

function stop() {
  PG_ID=$(containerId "postgres:9.6-alpine")
  if [ ! -z $PG_ID ]; then
      docker stop $PG_ID
  fi
  ZIPKIN_ID=$(containerId "openzipkin/zipkin")
  if [ ! -z $ZIPKIN_ID ]; then
      docker stop $ZIPKIN_ID
  fi
  KAFKA_ID=$(containerId "spotify/kafka")
  if [ ! -z $KAFKA_ID ]; then
      docker stop $KAFKA_ID
  fi
}

CMD=$1
OPTS="$2"
if [ -z $OPTS ]; then
   OPTS="min"
fi
case $CMD in
    start)
	start $OPTS
	;;
    stop)
	stop
	;;
    *)
        printf "Usage: \n./dev_dockers.sh start min/all \n ./dev_dockers stop"
	;;
esac
