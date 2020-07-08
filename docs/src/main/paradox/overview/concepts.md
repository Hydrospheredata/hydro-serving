# Concepts
There are a few concepts that you should be familiar with before starting to work with
 the Hydrosphere platform.

@@toc { depth=2 }


### Models & Model Versions

A model is a machine learning model or a processing function that consumes
provided inputs and produces predictions or transformations.

Within the Hydrosphere platform we break down the model to its versions.
Each **model version** represents a single Docker image containing all the artifacts
that you have uploaded to the platform. Consequently, **model** is a group of **model
 versions** with the same name.

### Runtimes

Each model relies on its runtime. A **runtime** is a separate service with the predefined
interface that is used to load and run your model. We have already @ref:[implemented
](../reference/runtimes.md) a few runtimes, which you can use in your own projects.

### Resource definitions and serving.yaml
Hydrosphere relies on resource definitions to build a reliable service from your
ML model. Resource definitions describe Models and Applications in terms
of runtime, payload and model signature in a YAML file. Typically we call these files 
`serving.yaml`. You can learn more about them in the @ref:[How to write resource definitions](../how-to/write-definitions.md) section


### Application

A model version itself is not capable of serving predictions. To do that, you would
have to create an application. An **application** is a publicly available endpoint
to reach your models. 

You can define complex pipelines where a single request goes
through multiple model versions. A pipeline exposes various endpoints to your models:
HTTP, gRPC, and Kafka. 

### Servable

**Servable** is an instance of a deployed model version with an exposed gRPC interface.
 You can access Servable through Gateway via HTTP or gRPC endpoints.


### Metrics 

Data coming through deployed Model Versions can be monitored with metrics. **Metric** is a Model Version
which takes a combination of inputs & outputs from an another, monitored Model Version,
receives every request and response from the monitored model,
produces a single value and compares it with a threshold to determine whether this request
was healthy or not.

Every request is evaluated against all metrics assigned to the model. Per-request metrics
allow you to make immediate judgments against an incoming request.

### Checks
A check is a boolean condition associated with a field of a Model Version signature which
 shows for every request whether field value is acceptable or not. e.g. Min/Max checks check
 that field value is in acceptable range, inferred from training data values.


### Model's Signature
Signature is a specification of your model computation which identifies the name of a
 function with its` inputs and outputs, including their names, shapes, and data types.

e.g. signature defined in a YAML file:
```yaml
contract:
  name: predict
  inputs:
    x:
      shape: [-1, 2]
      type: double
      profile: numerical
  outputs:
    y:
      shape: scalar
      type: int
      profile: categorical
```

### Field
Field is a basic element of a Model's signature. It has name, shape, data type and a profile.

e.g. model's signature field defined in a YAML file:
```yaml
x:
  shape: [-1, 2]
  type: double
  profile: numerical
```

### Field`s profile
Field's profile is a special tag which tells how Hydrosphere should interpret the field's data.

There are multiple available tags: Numerical, Categorical, Image, Text, Audio, Video etc.
