#!/bin/sh

SERVICE_ID=$1

cd $SERVER_HOME

chmod +x $PYTHON_START

exec python3 $PYTHON_START