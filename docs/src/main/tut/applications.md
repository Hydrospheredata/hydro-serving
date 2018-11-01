---
layout: docs
title:  "Applications"
permalink: 'applications.html'
---

# Applications 

__Application__ is a publicly available endpoint to reach your models. It allows you to use your most recent deployed production models via HTTP-requests, gRPC API calls or configure it as a part of Kafka streams. 

## Creating Applications

Open ML Lambda web interface, go to the `Applications` page and click `Add New` button. In the opened window you'll see basic configurations of the application. 

![]({{site.baseurl}}{%link /img/create-application-empty.png%})

<br>
When configuring applications, you have 2 options:

1. __Single-staged__ application. It's an option if you want to use just one of your models. In that case the model probably handles all necessary transformations and data cleaning itself and produces only the desired result. Or maybe you do the cleaning on your side, and you just need to get predicitons from the model (although in that case you might consider migrating pre/post-processing operations as __pipeline__ stages). 

2. __Multi-staged__ application. That's an option, if you want to create pipelines that will let data flow through different models, perform pre/post processing operations, etc.

## Invoking Applications

### Test request

You can perform test request to the model from interface. Open desired application, and press `Test` button. Internally it will generate input data from model's contract and send an HTTP-request to API endpoint. 

### HTTP-request

To let the model perform on your data through HTTP-request, you'll need to send `POST` request to `http://<host>/api/v1/applications/serve/{applicationId}/{applicationSignature}`. `applicationId` can be found in the address bar in the web interface (for example, `http://<host>/applications/1`, where `1` is the id of your application).
`applicationSignature` can be either original model's signature, if it's a __single-staged__ application, or the name of the application if it's a __multi-staged__ application. 

_Note: When you create a __multi-staged__ application, ML Lambda internally infers a contract. It performs validation that every stage is compatible with it's siblings, and creates a contract with the same signature name, as the application name. __Single-staged__ applications by default use their explicitly defined signatures._

### gRPC API call

You can define a gRPC client on your side, establish insecure connection with `http://<host>:8080` and make a call to `Predict` method. [Example Python client.]({{site.baseurl}}{%link getting-started.md%}#grpc-api-call)
