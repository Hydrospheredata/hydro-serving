# Concepts

There are a few concepts that you should be familiar with before starting to work with the Hydrosphere platform.

@@toc { depth=2 }


## Serving 

The concepts are related to the serving part of the Hydrosphere platform. 

### Models

A **Model** is a machine learning model or a processing function that consumes provided inputs and produces predictions or transformations. Within the Hydrosphere platform we break down the model to its versions. Each **Model version** represents a single Docker image containing all the artifacts that you have uploaded to the platform. 

### Runtimes

A **Runtime** is a docker image with the predefined GRPC interface and a server that can run your model. 
We have @ref:[implemented](../reference/runtimes.md) a few runtimes, which you can use in your own projects.

### Servable

**Servable** is a deployed instance of a Model version combined with a Runtime.
It exposes GRPC endoint that user can use to send requests.
User should not use Servable as-is, since it's are designed to be a building block, rather than inference endpoint.
We provide a better alternative to deploy a Model verision — Application.

### Applications

An **Application** is a pipeline of Servables.
When user creates Application, Manager service automatically deploys appropriate Servables.
Application handles monitoring of your models and is able to perform A/B traffic split. 

Servable can't do that. Don't deploy Servables unless you know what you are doing.

For each Application there are publicly avalilable HTTP and GRPC endpoints that you can send requests to.


## Monitoring 

The concepts described below are related to the monitoring part of the Hydrosphere platform. 

### Metrics 

User can monitor a model by assigning a set of metrics to it.

Metrics are grouped into the following categories: 

- **Per-Request Metrics** — Sonar sends every request to all assigned metrics. Per-request metrics allow you to make immediate judgments against an incoming request.
- **Batch Metrics** — Sonar collects requests into a batch. As soon as batch is collected, Sonar sends it to all assigned metrics for calculation.
- **Overall Metrics** — Sonar calculates metrics against all data that was collected during production inference. Overall metrics give you an idea of what all your data looks like through profile calculations. 