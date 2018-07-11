---
layout: docs
title:  "Applications"
permalink: 'application.html'
position: 6
---

__Application__ is a publicly available endpoint to reach your models. It allows you to use your most recent deployed production models via HTTP-requests, gRPC API calls, or configure it as a part of Kafka streams. 

# Creating Application

Open ML Lambda interface, go to the _Applications_ page and click _Add New_ button. In the opened window you'll see basic configurations of the application. 

![]({{site.baseurl}}{%link /img/create-application-empty.png%})
<sup>__Pic. 1__: Application's configurations.</sup>

| # | Element | Definition |
| - | ------- | ---------- |
| 1 |Application Name | Name of your application. |
| 2 | Use Kafka source | Option for configuring application as a node inside Kafka Streaming pipeline. Requires providing input/output topics. Can be configured with multiple topics. |
| 3 | Models | Framework, containing all models, used by applicaiton. |
| 4 | Stage_1 | Stage of the pipeline. |
| 5 | Model Selection | Model selection field. |
| 6 | Runtime | [Runtime]({{site.baseurl}}{%link runtime.md%}) selection field. |
| 7 | Signature | Field for describing which signature is going to be used from the ones, defined in the [contract]({{site.baseurl}}{%link contract.md%}) of the model. |
| 8 | Weight | The field, describing how much traffic model will receive with respect to other models inside the stage. Mainly used for A/B testing or canary traffic split. |
| 9 | Add model versions | Option for adding additional models to the stage. |
| 10 | Add New Stage | Button for creating additional stages for application. |
| 11 | Add Application | Button for creating an application. |

When configuring applications, you have 2 options:

1. __Single-staged__ application. It's an option if you want to use just one of your models. In that case the model probably handles all necessary transformations and data cleaning itself and produces only the desired result. Or maybe you do the cleaning on your side, and you just need to get predicitons from the model (although in that case you might consider migrating pre/post-processing operations as __pipeline__ stages). 

2. __Multi-staged__ application. That's an option, if you want to create pipelines that will let data flow through different models, perform pre/post processing operations, etc.

# Invoking Application

## Test-request
When you have just created an application and want to test it's performance, you can run it from the web-interface. Go to `Applications`, open desired app in the sidebar, press `Test` button.

![]({{site.baseurl}}{%link /img/ui-test-button.png%})
<sup>__Pic. 2__: Testing application.</sup>

## HTTP-request

To let the model perform on your data through HTTP-request, you'll need to send `POST` request to `/api/v1/applications/serve/{applicationId}/{signatureName}`. Depending on the type of your application (Single-staged, Multi-staged), you may need to use different signatures. 

> Note: When you create a __Multi-staged__ application, ML Lambda internally infers a contract. It performs validation that every stage is compatible with it's siblings, and creates a contract with the same signature name, as the application name. __Single-staged__ applications by default use their explicitly defined signatures. 
