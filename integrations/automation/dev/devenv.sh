#!/bin/bash

# usage: `source devenv.sh`
# host ip - tested nly for linux

echo "Setting up variables"
export HOST_IP=$(route | grep default | awk '{print $8}' | head -n 1 | xargs ifconfig | grep inet | head -n 1 | awk '{print $2}')
echo "HOST_IP:"$HOST_IP

FULL_PATH=$(realpath $(dirname ${BASH_SOURCE[${#BASH_SOURCE[@]} - 1]}))
export MODEL_DIRECTORY=$(realpath $FULL_PATH/../../../../hydro-serving-runtime/models)
echo "MODEL_DIRECTORY:"$MODEL_DIRECTORY

