#!/bin/sh

VERSION=$1

mkdir -p target/package

cp -r ./scripts/* target/package/

git clone --branch release-2.3.0 https://github.com/megastep/makeself target/makeself

docker rm -f envoy_build
docker run --name envoy_build lyft/envoy-alpine:e366bcfd8dee9d4f2eb3956e77a77f17bd7a5244
docker cp envoy_build:/usr/local/bin/envoy target/package/
docker rm -f envoy_build

target/makeself/makeself.sh target/package target/hydro-serving-sidecar-install-$VERSION.sh "Hydro-Serving Package" ./install_dependencies.sh
chmod +x target/hydro-serving-sidecar-install-$VERSION.sh
