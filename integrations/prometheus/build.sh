#!/bin/sh

VERSION=$1
[ -z "$VERSION" ] && VERSION="0.0.1"

docker build --no-cache -t hydro-serving/prometheus:$VERSION .