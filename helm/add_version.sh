#!/usr/bin/env bash

DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

cat >> index.yaml << EOM
  - apiVersion: v2
    created: ${DATE}
    description: Hydrosphere Serving is a cluster for deploying and versioning your machine learning models in production
    digest: ${2}
    name: serving
    urls:
    - https://github.com/Hydrospheredata/hydro-serving/releases/download/${1}/serving-${1}.tgz
    version: ${1}
    appVersion: ${1}
EOM

