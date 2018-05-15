#!/bin/bash

COMPOSE_FILE=$1
[ -z "$COMPOSE_FILE" ] && COMPOSE_FILE="docker-compose.yml"