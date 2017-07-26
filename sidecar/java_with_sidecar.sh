#!/usr/bin/env sh

VERSION=$1

docker build --no-cache --build-arg VERSION="$VERSION" -f Java_Dockerfile -t hydro-serving/java:$VERSION .