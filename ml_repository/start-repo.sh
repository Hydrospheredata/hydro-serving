#!/bin/sh

SERVICE_ID=$1

cd $SERVER_HOME

exec java $JAVA_OPTS -jar $SERVER_JAR