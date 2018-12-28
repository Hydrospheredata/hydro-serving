---
layout: docs
title:  "Write Manifests"
permalink: 'write-manifests.html'
---

# Write Manifests

Manifests describe entities inside ML Lambda. They can define your models, applications, runtimes and environments. Each manifest has the 2 following mandatory fields. 

```yaml
kind: ...
name: "example"
```

The fields are self-explanatory: 

- `kind` defines the kind of the entity;
- `name` defines the name of the entity.

Let's take a look at the entity kinds. 

<br> 

### `kind: Model`

For this type of the manifest you have to declare:

- `model-type` defines the model type. Will be matched with the runtime's `model-type`;
- `payload` defines all files for the model;
- `contract` defines a collection of signatures. A signature is a supported computation on the model.

```yaml
kind: Model
name: sample_model
model-type: "python:3.6"
payload: 
  - "./*"
contract:
  infer:
    inputs:
      input_field_1:
        shape: [-1, 1]
        type: string
        profile: text
      input_field_2:
        shape: scalar
        type: int32
        profile: numeric
    outputs: 
      output_field_1:
        shape: [-1, 2]
        type: int32 
        profile: numeric
```

In the example above we've defined a signature with `infer` name. Each signature has to have `inputs` and `outputs`. They define what kind of data the model will receive and what will it produce. Each input and output field has a 3 defined properties - `shape`, `type` and `profile`. You can look up all their possible values in the [reference]({{site.baseurl}}{%link reference/manifests.md%}#kind-model). 

<br>

### `kind: Application`

For this type of the manifest you have to declare one of the follwing:

- `singular` defines a single-model application. 
- `pipeline` defines application as a pipeline of models.  

<hr>

`singular` applications usually consist of smaller amout of definitions. 

```yaml
kind: Application
name: sample_application
singular:
  monitoring:
    - name: Concept Drift
      input: input_field_1
      type: Autoencoder
      app: sample_application_autoencoder
      healthcheck:
        enabled: true
        threshold: 0.15
    - name: KS
      input: input_field_2
      type: Kolmogorov-Smirnov
      healthcheck:
        enabled: true
  model: sample_model:1
  runtime: hydrosphere/serving-runtime-python:3.6-latest
```

`singular` field has three properties:
- `model` defines the model and its version to use. Expected to be in the form `model-name:model-version`;
- `runtime` defines the runtime to run the model on;
- `monitoring` defines the list of monitoring metrics applied to the model. _Optional_.

`monitoring` metric can have 5 fields: 
- `name` defines the name of the metric;
- `input` defines which input field to monitor from the model;
- `type` defines the type of the metric;
- `healthcheck` defines, if the metric should be used as a helthcheck for the model. By default healthcheck is disabled.<br>For some of the models you can also set `threshold`. 
- `app` defines what application to use for inference for the metric. _Optional_.

<hr>

`pipeline` applications have more detailed definitions.

```yaml
kind: Application
name: sample-claims-app
pipeline:
  - signature: claims
    model: claims-preprocessing:1
    runtime: hydrosphere/serving-runtime-python:3.6-latest
  - signature: claims
    modelservices:
      - model: claims-model:1
        runtime: hydrosphere/serving-runtime-tensorflow:1.7.0-latest
        weight: 80
      - model: claims-model-old:2
        runtime: hydrosphere/serving-runtime-tensorflow:1.7.0-latest
        weight: 20
    monitoring:
      - name: gan
        input: feature
        type: GAN
        app: claims-autoencoder-app
        healthcheck:
          enabled: true
          treshold: 69
```

`pipeline` is a list of stages. Each item in the list can have the following attributes:
- `signature` defines which signature to use from the model
- `model` defines the model and its version to use. Expected to be in the form `model-name:model-version`;
- `runtime` defines the runtime to run the model on;
- `monitoring` defines the metric to use on the stage;
- A stage can consist of multiple models. In that case you can define `modelservices` where you will list needed models. For each model in you would have to declare a `weight` attribute, which has to sum up to 100 across all the models in the stage. The `weight` defines how much traffic would go through the model.

<br>

### `kind: Runtime`

For this type of the manifest you have to declare:

- `model-type` defines which type of the models can be run on the runtime;
- `version` defines the tag of the Docker image to use.

```yaml
kind: Runtime
name: hydrosphere/serving-runtime-python
version: 3.6-latest
model-type: python:3.6
```

Note, that in this manifest the `name` field has also another meaning. It refers to the `{username}/{image_name}` available somewhere at the public docker registry. 

<br>

### `kind: Environment`

Available soon. 

<br>
<hr>

# What's next? 

- [Learn, how to serve Python models]({{site.baseurl}}{%link tutorials/python.md%});
- [Learn, how to serve Tensorflow models]({{site.baseurl}}{%link tutorials/tensorflow.md%});


