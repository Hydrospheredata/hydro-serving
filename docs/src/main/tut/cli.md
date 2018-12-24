---
layout: docs
title:  "CLI"
permalink: 'cli.html'
---

# Command Line Interface

To work with ML Lambda from the command line we've created `hs`. It allows you to _apply_ various resource configurations to the platform. 

## Installation

If you haven't installed it yet, you can do it by `pip install hs`.

<hr>

### `hs cluster`

Cluster is a space, where you deploy your models. Simply put, it points where to apply your configurations and where to upload your models. It stores all the clusters in `~/.hs-home/config.yaml` file.

```yaml
# Example configuration

kind: Config
clusters:
  - cluster: 
      server: "http://127.0.0.1"
    name: local
current-cluster: local
```

To look up, which cluster do you use, use:

```sh
$ hs cluster
``` 

To add a new cluster, use:

```sh
$ hs cluster add --name new_local --server http://localhost:16000/
```

To switch to the new cluster, use:

```sh
$ hs cluster use new_local
```

To remove the new cluster, use:

```sh
$ hs cluster rm new_local
```

<hr>

### `hs upload`

When you upload a model, the tool looks for `serving.yaml` file in current directory. `serving.yaml` defines model's metadata and it's contract. For more information, check out [contracts]({{site.baseurl}}{%link concepts/models.md%}#contracts).

```yaml
# Example model

kind: Model
name: "example_model"
model-type: "tensorflow:1.3.0"
payload:
  - "saved_model.pb"
  - "variables/"
  
contract:
  detect:                   # the name of signature
    inputs:                 # signature input fields
      image_b64:
        type: string
    outputs:                # signature output fields
      scores:
        shape: [-1]
        type: double
        profile: numerical
      classes:
        shape: [-1]
        type: string
        profile: numerical
```

<hr>

### `hs apply` 

CLI supports a specific set of resources available to `apply`:

- Model
- Runtime
- Environment
- Application

These resources are detected by `kind` key in the file.

### Model

```yaml
# Example model

version: v2-alpha
kind: Model
name: foo-model-script
model-type: python:3.6
payload:
  - /models/my-model/
contract:
  claim:
    inputs:
      client_profile:
        shape: [112]
        type: float32 
        profile: text 
    outputs:
      amount:
        shape: scalar
        type: int64
        profile: real
```

### Runtime

```yaml
# Example runtime

version: v2-alpha
kind: Runtime
name: hydrosphere/serving-runtime-tensorflow
version: 1.7.0-latest
model-type: tensorflow:1.7.0
```

### Environment

```yaml
# Example environment 

version: v2-alpha
kind: Environment
name: xeon-cpu
selector: "/* AWS INSTANCE SELECTOR */"
```

### Application

For the sake of simplicity CLI provides simplified structures for major use cases:

1. __Single-model application__

    ```yaml
    # Example application

    version: v2-alpha
    kind: Application
    name: singular-application
    streaming:
      in-topic: input-topic
      out-topic: output-topic

    singular:                         # keyword for single model application
      monitoring:                     # monitoring parameters
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
      modelservice:                   # ModelService definition
        model:
          name: claims-model
          version: 1
        runtime:
          name: hydrosphere/serving-runtime-python
          version: 3.6-latest
        environment: intel-xeon
    ```

    _Note: The config is less heavy and removes unnecessary fields such as `weight` and `signature`._

1. __Pipeline application__

    ```yaml
    # Example application 

    version: v2-alpha
    kind: Application
    name: claims-pipeline-app
    streaming:
      in-topic: claims-input
      out-topic: claims-output

    pipeline:                         # keyword for pipeline app
      1-preprocessing:                # named stage definition
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