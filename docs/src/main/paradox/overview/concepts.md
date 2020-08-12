# Concepts
There are a few concepts that you should be familiar with before starting to work with
 the Hydrosphere platform.

@@toc { depth=2 }


### Models & Model Versions

A **Model** is a machine learning model or a processing function that consumes provided
inputs and produces predictions or transformations. 

Within the Hydrosphere platform we break down the model to its versions.
Each **Model version** represents a single Docker image containing all the artifacts
that you have uploaded to the platform. Consequently, **Model** is a group of **Model
 versions** with the same name.

### Runtimes

A **Runtime** is a docker image with the predefined gRPC interface which loads and serves
 your model.
 
We have @ref:[implemented](../reference/runtimes.md) a few runtimes, which you can use
 in your own projects.

### Servable

**Servable** is a deployed instance of a Model version combined with a Runtime.
It exposes a gRPC endpoint that user can use to send requests.

User should not use Servable as-is, since it's are designed to be a building block,
rather than inference endpoint. We provide a better alternative to deploy a Model
 version — Application.

### Applications

An **Application** is a pipeline of Servables.

When user creates Application, Manager service automatically deploys appropriate Servables.
Application handles monitoring of your models and is able to perform A/B traffic split. 

Servable can't do that. Don't deploy Servables unless you know what you are doing.

For each Application there are publicly available HTTP and gRPC endpoints that you
 can send requests to.

## Deployment Configurations
A **Deployment Configuration** is a collection of Kubernetes settings which you can set
 for your Servables and Model Versions used in stages of Applications.
 
Deployment Configuration covers:

* Horizontal Pod Autoscaler specs
* Container Specs 
    ** Resource requirements - limits and requests 
* Pod Specs
    * Node Selectors
    * Affinity
    * Tolerations
* Deployment Specs
    * Replicas count

### Resource definitions
Hydrosphere relies on **Resource definitions** to build a reliable service from your
ML model. 

**Resource definitions** describe Models, Applications and Deployment Configurations in
a YAML format. You can learn more about them in the
 @ref:[How to write resource definitions](../how-to/write-definitions.md) section.


### Metrics 

Data coming through deployed Model Versions can be monitored with metrics.
 
**Metric** is a Model Version which takes a combination of inputs & outputs from
an another monitored Model Version, receives every request and response from the monitored
model, produces a single value and compares it with a threshold to determine whether
this request was healthy or not.

Every request is evaluated against all metrics assigned to the model.

### Checks
A check is a boolean condition associated with a field of a Model Version signature which
shows for every request whether field value is acceptable or not. 

e.g. Min/Max checks ensure that field value is in acceptable range which is inferred
 from training data values.


### Model's Signature
Signature is a specification of your model computation which identifies the name of a
 function with its inputs and outputs, including their names, shapes, and data types.

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
