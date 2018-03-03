#!/bin/bash

./build-containers.sh

pushd integrations/automation/demo

pushd noteboks
git clone https://github.com/balancap/SSD-Tensorflow
popd

docker-compose up -d
popd