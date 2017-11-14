#!/bin/bash

function containerId() {
  NAME=$1
  docker ps -a | grep $NAME | awk '{print $1}' | head -n 1
}

function start() {
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
}

function stop() {
  PG_ID=$(containerId "postgres:9.6-alpine")
  if [ ! -z $PG_ID ]; then
      docker stop $PG_ID
  fi
  ZIPKIN_ID=$(containerId "openzipkin/zipkin")
  if [ ! -z $PG_ID ]; then
      docker stop $ZIPKIN_ID
  fi
}

CMD=$1
case $CMD in
    start)
	start
	;;
    stop)
	stop
	;;
    *)
        echo "Usage: ./dev_dockers.sh start/stop"
	;;
esac
