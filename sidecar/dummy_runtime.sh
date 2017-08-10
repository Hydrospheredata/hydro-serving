#!/bin/sh

VERSION=$1

docker build --no-cache --build-arg VERSION="$VERSION" -f DummyRuntime_Dockerfile -t hydro-serving/dummy-runtime:$VERSION .