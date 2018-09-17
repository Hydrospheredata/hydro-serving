---
layout: docs
title:  "CLI"
permalink: 'cli.html'
---

# Hydroserving command line interface

CLI can `apply` various resource configurations to the platform.

But you need to set it up beforehand.

## CLI configuration

CLI stores configurations in `~/.hs-home/config.yaml` file.

Example config:
```yaml
kind: Config
clusters:
  - cluster: 
      server: "http://127.0.0.1"
    name: local
current-cluster: local
```

You can set current cluster to work with via `hs cluster use $CLUSTER_NAME` command.

## Apply 

CLI supports specific set of resources available to `apply`:

- Model
- Runtime
- Environment
- Application

These resources are detected by `kind` key in the file.

### Model
Apply will use the same config as in `serving.yaml` metadata file. 
For more details see `hs upload` page.

Example:

```yaml
version: v2-alpha
kind: Model
name: foo-model-script
model-type: python:3.6
payload:
  - /models/my-model/src/
contract:
  claim:
    inputs:
      client_profile:
        shape: [112]
        type: float32 # using tf type names instead of proto ones
        profile: text # data profiling tags
    outputs:
      amount:
        shape: scalar
        type: int64
        profile: real
```

### Runtime

Runtime definition.

Example:

```yaml
version: v2-alpha
kind: Runtime
name: hydrosphere/serving-runtime-tensorflow
version: 1.7.0-latest
model-type: tensorflow:1.7.0
```

### Environment

Environment definition.

Example:

```yaml
version: v2-alpha
kind: Environment
name: xeon-cpu
selector: "/* AWS INSTANCE SELECTOR */"
```

### Application

For the sake of simplicity CLI provides simplified structures for major use cases:

### Single model application

As described below, the config removes unnecessary fields such as `weight` and `signature`.

Example: 

```yaml
version: v2-alpha
kind: Application
name: singular-application
streaming:
  in-topic: input-topic
  out-topic: output-topic

singular: # keyword for single model application
  monitoring: # monitoring parameters
    ks:
      input: feature_42
      type: Kolmogorov-Smirnov
      is_healthcheck: true
      treshold: 69
    gan:
      input: feature
      type: GAN
      app: claims-autoencoder-app
      is_healthcheck: true
      treshold: 69
    autoencoder:
      input: feature
      type: Autoencoder
      app: claims-autoencoder-app
      is_healthcheck: true
      treshold: 69
  modelservice: # ModelService definition
    model:
      name: claims-model
      version: 1
    runtime:
      name: hydrosphere/serving-runtime-python
      version: 3.6-latest
    environment: intel-xeon
```

### Pipeline application

Example:

```yaml
version: v2-alpha
kind: Application
name: claims-pipeline-app
streaming:
  in-topic: claims-input
  out-topic: claims-output

pipeline: # keyword for pipeline app
  1-preprocessing: # named stage definition
    signature: claims
    modelservice:
      model:
          name: claims-preprocessing
          version: 1
      runtime:
        name: hydrosphere/serving-runtime-python
        version: 3.6-latest
      environment: cpu

  2-model:
    signature: claims
    monitoring:
      ks:
        input: feature_42
        type: Kolmogorov-Smirnov
        is_healthcheck: true
      gan:
        input: feature
        type: GAN
        app: claims-autoencoder-app
        is_healthcheck: true
      autoencoder:
        input: feature
        type: Autoencoder
        app: claims-autoencoder-app
        is_healthcheck: true
    modelservices:
      simple-xeon:
        model:
          name: claims-model
          version: 1
        runtime:
          name: hydrosphere/serving-runtime-python
          version: 3.6-latest
        environment: xeon
      test-old:
        model:
          name: claims-model-old
          version: 2
        runtime:
          name: hydrosphere/serving-runtime-python
          version: 3.6-latest
    weights:
      simple-xeon: 80
      test-old: 20
```