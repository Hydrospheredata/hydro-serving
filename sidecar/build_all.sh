#!/usr/bin/env sh

VERSION=$1

pwd

./side_car_package.sh $VERSION
./java_with_sidecar.sh $VERSION
./dummy_runtime.sh $VERSION