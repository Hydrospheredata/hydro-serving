# Concepts

There are a few concepts that you should be familiar with before starting to work with the Hydrosphere platform.

@@toc { depth=2 }


## Serving 

Concepts described below are related to a serving part of the Hydrosphere platform. 

### Models

A **model** is a machine learning model or a processing function that consumes provided inputs and produces predictions or transformations. Within the Hydrosphere platform we break down the model to its versions. Each **model version** represents a single Docker image containing all the artifacts that you have uploaded to the platform. 

### Runtimes

Each model relies on its runtime. A **runtime** is a separate service with the predefined interface that is used to run your model. We have already @ref:[implemented](../reference/runtimes.md) a few runtimes, which you can use in your own projects.

### Applications

A model version itself is not capable of serving predictions. To do that you would have to create an application. An **application** is a publicly available endpoint to reach your models. You can define complex pipelines where a single request goes through multiple model versions. A pipeline exposes various endpoints to your models: HTTP, gRPC, and Kafka. 


## Monitoring 

Concepts described below are related to a monitoring part of the Hydrosphere platform. 

### Metrics 

Every model can be monitored with a set of metrics. Conceptually metrics can be grouped into the following categories: 

- **Per-Request Metrics** — every request is evaluated against all assigned to the model metrics. Per-request metrics allow you to make immediate judgments against an incoming request.
- **Batch Metrics** — requests are collected into a batch and the batch is proceeded to the assigned metrics for the the calculation. 
- **Overall Metrics** — metrics are calculated against all data that was collected during production inference. Overall metrics give you an idea how all of your data looks like through profile calculations. 