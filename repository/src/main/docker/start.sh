#!/bin/sh

SERVICE_ID=$1

JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

cd /hydro-serving/app
exec java $JAVA_OPTS -jar repository.jar