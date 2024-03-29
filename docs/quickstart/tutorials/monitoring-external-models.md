# Monitoring External Models

## Overview

Monitoring can be used to track the behavior of external models running outside of the Hydrosphere platform. This tutorial describes how to register an external model, trigger analysis over your requests, and retrieve results.

By the end of this tutorial you will know how to:

* Register a model
* Upload training data
* Assign custom metrics
* Invoke analysis
* Retrieve metrics

## Prerequisites

For this tutorial, you need to have **Hydrosphere Platform** deployed on your local machine with **Sonar** component enabled. If you don't have it yet, please follow this guide first:

* [Platform Installation](../installation/)

You also need a running external model, capable of producing predictions. Inputs and outputs of that model will be fed into Hydrosphere for monitoring purposes.

## Model registration

First, you have to register an external model. To do that, submit a JSON document, defining your model.

### Request document structure

This section describes the structure of the JSON document used to register external models within the platform.

#### Top-level members

The document **must** contain the following top-level members, describing the interface of your model:

* `name`: the name of the registered model. This name uniquely identifies a collection of model versions, registered within the Hydrosphere platform.
* `signature`: the interface of the registered model. This member describes inputs and outputs of the model, as well as other complementary metadata, such as data profile for each field.  

A document **may** contain additional top-level members, describing other details of your model.

* `metadata`: the metadata of the registered model. The structure of the object is not strictly defined. The only constraint is that the object must have a key-value structure, where a value can only be of a simple data type \(string, number, boolean\).
* `monitoringConfiguration`: monitoring configuration to be used for this model. 

This example shows, how a model can be defined at the top level:

```javascript
{  
    "name": "external-model-example",
    "metadata": {
        "architecture": "Feed-forward neural network",
        "description": "Sample external model example",
        "author": "Hydrosphere.io",
        "training-data": "s3://bucket/external-model-example/data/",
        "endpoint": "http://example.com/api/external-model/"
    },
    "monitoringConfiguration": {
        "batchSize": 100
    },
    "signature": {
        ...
    }
}
```

#### MonitoringConfiguration object

`monitoringConfiguration` object defines a monitoring configuration to be used for the model version. The object **must** contain the following members:

* `batchSize`: size of the batch to be used for aggregations.

The example below shows how a `monitoringConfiguration` object can be defined.

```javascript
{
    "batchSize": 100,
}
```

#### Signature object

`signature` object describes the signature of the model. The signature object **must** contain the following members:

* `signatureName`: The signature of the model, used to process the request;
* `inputs`: A collection of fields, defining the inputs of the model. Each item in the collection describes a single data entry, its type, shape, and profile. A collection **must** contain at least one item;
* `outputs`: A collection of fields, defining the outputs of the model. Each item in the collection describes a single data entry, its type, shape, and profile. A collection **must** contain at least one item. 

The example below shows how a `predict` object can be defined.

```javascript
{
    "signatureName": "predict",
    "inputs": [
        ...
    ],
    "outputs": [
        ...
    ]
}
```

#### Field object

Items in the `inputs` / `outputs` collections are collectively called "fields". The field object **must** contain the following members:

* `name`: Name of the field;
* `dtype`: Data type of the field. 
* `profile`: Data profile of the field. 
* `shape`: Shape of the field.

The only _valid_ options for `dtype` are:

* DT\_STRING;
* DT\_BOOL;
* DT\_VARIANT;
* DT\_HALF;
* DT\_FLOAT;
* DT\_DOUBLE;
* DT\_INT8;
* DT\_INT16;
* DT\_INT32;
* DT\_INT64;
* DT\_UINT8;
* DT\_UINT16;
* DT\_UINT32;
* DT\_UINT64;
* DT\_QINT8;
* DT\_QINT16;
* DT\_QINT32;
* DT\_QUINT8;
* DT\_QUINT16;
* DT\_COMPLEX64;
* DT\_COMPLEX128;

The only _valid_ options for `profile` are:

* NONE
* NUMERICAL
* TEXT
* IMAGE

The example below shows how a single `field` object can be defined.

```javascript
{
    "name": "age",
    "dtype": "DT_INT32",
    "profile": "NUMERICAL",
    "shape": {
        ...
    }
}
```

#### Shape object

`shape` object defines the shape of the data that the model is processing. The shape object **must** contain the following members:

* `dims`: An array of dimensions. A collection **may** be empty — in that case, the tensor will be interpreted as a scalar value. 

The example below shows how a `shape` object can be defined.

```javascript
"shape": {
    "dims": [-1, 64, 64],
}
```

### Registering external model

A model can be registered by sending a `POST` request to the `/api/v2/externalmodel` endpoint. The request **must** include a model definition as primary data.

The request below shows an example of an external model registration.

```javascript
POST /api/v2/externalmodel HTTP/1.1
Content-Type: application/json
Accept: application/json

{
    "name": "external-model-example",
    "metadata": {
        "architecture": "Feed-forward neural network",
        "description": "Sample external model example",
        "author": "Hydrosphere.io",
        "training-data": "s3://bucket/external-model-example/data/",
        "endpoint": "http://example.com/api/external-model/"
    },
    "monitoringConfiguration": {
        "batchSize": 100
    },
    "signature": {
        "signatureName": "predict",
        "inputs": [
            {
                "name": "in",
                "dtype": "DT_DOUBLE",
                "profile": "NUMERICAL",
                "shape": {
                    "dims": []
                }
            }
        ],
        "outputs": [
            {
                "name": "out",
                "dtype": "DT_DOUBLE",
                "profile": "NUMERICAL",
                "shape": {
                    "dims": []
                }
            }
        ]
    }
}
```

As a response, the server will return a JSON object with complementary metadata, identifying a registered model version.

### Response document structure

The response object from the external model registration request contains the following fields:

* `id`: Model version ID, uniquely identifying a registered model version within Hydrosphere platform;
* `model`: An object, representing a model collection, registered in Hydrosphere platform;
* `modelVersion`: Model version number in the model collection; 
* `signature`: Contract of the model, similar to the one defined in the request section above;
* `metadata`: Metadata of the model, similar to the one defined in the request section above;
* `monitoringConfiguration`: MonitoringConfiguration of the model, similar to the one defined in the request section above;
* `created`: Timestamp, indicating when the model was registered. 

{% hint style="info" %}
Note the`id` field. It will be referred as `MODEL_VERSION_ID` later throughout the article.
{% endhint %}

#### Model object

`model` object represents a collection of model versions, registered in the platform. The response `model` object contains the following fields:

* `id`: ID of the model collection;
* `name`: Name of the model collection.

The example below shows, a sample server response from an external model registration request.

```javascript
HTTP/1.1 200 OK
Content-Type: application/json

{
    "id": 1,
    "model": {
        "id": 1,
        "name": "external-model-example"
    },
    "modelVersion": 1,
    "created": "2020-01-09T16:25:02.915Z",
    "signature": { 
        "signatureName": "predict",
        "inputs": [
            {
                "name": "in",
                "dtype": "DT_DOUBLE",
                "profile": "NUMERICAL",
                "shape": {
                    "dims": []
                }
            }
        ],
        "outputs": [
            {
                "name": "out",
                "dtype": "DT_DOUBLE",
                "profile": "NUMERICAL",
                "shape": {
                    "dims": []
                }
            }
        ]
    },
    "metadata": { 
        "architecture": "Feed-forward neural network",
        "description": "Sample external model example",
        "author": "Hydrosphere.io",
        "training-data": "s3://bucket/external-model-example/data/",
        "endpoint": "http://example.com/api/external-model/"
    },
    "monitoringConfiguration": {
        "batchSize": 100
    }
}
```

## Training data upload

To let Hydrosphere calculate the metrics of your requests, you have to submit the training data. You can do so by:

* [using CLI](monitoring-external-models.md#upload-using-cli)
* [using HTTP endpoint](monitoring-external-models.md#upload-using-http-endpoint)

In each case your training data should be represented as a CSV document, containing fields named exactly as in the [interface](monitoring-external-models.md#request-document-structure) of your model.

{% hint style="info" %}
Currently, we support uploading training data as .csv files and utilizing it for NUMERICAL, CATEGORICAL, and TEXT profiles only.
{% endhint %}

### Upload using CLI

Switch to the cluster, suitable for your current flow.

```bash
$ hs cluster use example-cluster
Switched to cluster '{'cluster': {'server': '<hydrosphere>'}, 'name': 'example-cluster'}'
```

If you don't have a defined cluster yet, create one using the following command.

```text
$ hs cluster add --server <hydrosphere> --name example-cluster
Cluster 'example-cluster' @ <hydrosphere> added successfully
$ hs cluster use example-cluster
```

Make sure you have a local copy of the training data that you want to submit.

```text
$ head external-model-data.csv
in,out
0.8744973,0.74737076
0.35367096,0.68612554
0.12600919,0.23873545
0.22988156,0.01602719
0.09958467,0.81491237
0.50324137,0.23527377
0.02184051,0.37468397
0.23937149,0.66311923
0.48611933,0.65467976
0.98475208,0.28292798
```

Submit the training data. You **must** specify two parameters:

* `--model-version`: A string indicating the model version to which you want to submit the data. The string should be formatted in the following way `<model-name>:<model-version>`;
* `--filename`: Path to a filename, that you want to submit. 

{% hint style="info" %}
If you already have your training data uploaded to S3, you can specify a path to that object URI using `--s3path` parameter instead of `--filename`. The object behind this URI should be available to the Hydrosphere instance.
{% endhint %}

```text
$ hs profile push \
    --model-version external-model-example:1 \
    --filename external-model-data.csv
```

Depending on the size of your data, you will have to wait for the data to be uploaded. If you don't want to wait, you can use the `--async` flag.

### Upload using an HTTP endpoint

To upload your data using an HTTP endpoint, stream it to the `/monitoring/profiles/batch/<MODEL_VERSION_ID>` endpoint.

```shell
curl -v -D - -X POST -H "Transfer-Encoding: chunked" -H "Content-Type: text/plain" --compressed --data-binary @file.csv <HYDROSPHERE_ADDRESS>/monitoring/profiles/batch/<MODEL_VERSION_ID>
```

{% hint style="info" %}
You can acquire `MODEL_VERSION_ID` by sending a GET request to `/model/version/<MODEL_NAME>/<MODEL_VERSION>` endpoint. The response document will have a similar structure, already defined @ref[above](monitoring-external-models.md#response-document-structure).
{% endhint %}

## Custom metrics assignment

This step is **optional**. If you wish to assign a custom monitoring metric to a model, you can do it by:

* using Hydrosphere UI
* using HTTP endpoint

### Using Hydrosphere UI

To find out how to assign metrics using Hydrosphere UI, refer to [this](custom_metric.md#ui) page.

### Using HTTP endpoint

To assign metrics using HTTP endpoint, you will have to submit a JSON document, defining a monitoring specification.

#### Top-level members

The document **must** contain the following top-level members.

* `name`: The name of the monitoring metric;
* `modelVersionId`: Unique identifier of the model **to which** you want to assign a metric;
* `config`: Object, representing a configuration of the metric, **which** will be applied to the model. 

The example below shows how a metric can be defined on the top level.

```javascript
{
    "name": "string",
    "modelVersionId": 1,
    "config": {
        ...
    }
}
```

#### Config object

`config` object defines a configuration of the monitoring metric that will monitor the model. The model **must** contain the following members:

* `modelVersionId`: Unique identifier of the model **that** will monitor requests;
* `threshold`: Threshold value, against which monitoring values will be compared using a comparison operator;
* `thresholdCmpOperator`: Object, representing a comparison operator. 

The example below shows, how a metric can be defined on a top-level.

```javascript
{
    "modelVersionId": 2,
    "threshold": 0.5,
    "thresholdCmpOperator": {
        ...
    }
}
```

#### ThresholdCmpOperator object

`thresholdCmpOperator` object defines the kind of comparison operator that will be used when comparing a value produced by the metric against the threshold. The object **must** contain the following members:

* `kind`: Kind of comparison operator.

The only _valid_ options for `kind` are:

* Eq;
* NotEq;
* Greater;
* Less;
* GreaterEq;
* LessEq. 

The example below shows, how a metric can be defined on the top level.

```javascript
{
    "kind": "LessEq"
}
```

The request below shows an example of assigning a monitoring metric. At this moment, both monitoring and the actual prediction model should be registered/uploaded to the platform.

```javascript
POST /monitoring/metricspec HTTP/1.1
Content-Type: application/json
Accept: application/json

{
    "name": "string",
    "modelVersionId": 1,
    "config": {
        "modelVersionId": 2,
        "threshold": 0.5,
        "thresholdCmpOperator": {
            "kind": "LessEq"
        }
    }
}
```

## Analysis invocation

Monitoring service has GRPC API that you can use to send data for analysis. Here is how it works:

0. Need to use compiled GRPC services. We provide [_libraries_](../../resources/reference/libraries.md) with precompiled services. If your language is not there, you need to compile GRPC yourself.
1. Create an [_ExecutionMetadata_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/b8b4b1ff4a86c81bc8d151194f522a5e9c487af8/src/hydro_serving_grpc/monitoring/sonar/entities.proto#L13) message that contains information of the model that was used to process a given request.
2. Create a [_PredictRequest_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/b8b4b1ff4a86c81bc8d151194f522a5e9c487af8/src/hydro_serving_grpc/serving/runtime/api.proto#L8) message that contains the original request passed to the model for prediction.
3. If model responded successfully then create a [_PredictResponse_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/b8b4b1ff4a86c81bc8d151194f522a5e9c487af8/src/hydro_serving_grpc/serving/runtime/api.proto#L14) message that contains inferenced output of the model. If there was an error, then instead of a _PredictResponse_ message  you should prepare an error message.
4. Assemble an [_ExecutionInformation_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/b8b4b1ff4a86c81bc8d151194f522a5e9c487af8/src/hydro_serving_grpc/monitoring/sonar/entities.proto#L24) using the messages above.
5. Submit _ExecutionInformation_ proto to Sonar for analysis. Use the RPC [_MonitoringService.Analyze_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/b8b4b1ff4a86c81bc8d151194f522a5e9c487af8/src/hydro_serving_grpc/monitoring/sonar/api.proto#L10) method to calculate metrics.

## Metrics retrieval

Once triggered, the [MonitoringService.Analyze](https://github.com/Hydrospheredata/hydro-serving-protos/blob/b8b4b1ff4a86c81bc8d151194f522a5e9c487af8/src/hydro_serving_grpc/monitoring/sonar/api.proto#L10) method does not return anything. To fetch calculated metrics from the model version, you have to make a GET request to the `/monitoring/checks/all/<MODEL_VERSION_ID>` endpoint.

A request **must** contain the following parameters:

* `limit`: how many requests to fetch;
* `offset`: which offset to make from the beginning.

An example request is shown below.

```javascript
GET /monitoring/checks/all/1?limit=1&offset=0 HTTP/1.1
Accept: application/json
```

Calculated metrics have a dynamic structure, which is dependant on the model interface.

### Response object structure

A response object contains the original data submitted for prediction, the model's response, calculated metrics and other supplementary metadata. Every field produced by Hydrosphere is prefixed with `_hs_` char.

* `_id`: ID of the request, generated internally by Hydrosphere; 
* `_hs_request_id`: ID of the request, specified by user;
* `_hs_model_name`: Name of the model that processed a request;
* `_hs_model_incremental_version`: Version of the model that processed a request; 
* `_hs_model_version_id`: ID of the model version, which processed a request;
* `_hs_raw_checks`: Raw checks calculated by Hydrosphere based on the training data;
* `_hs_metric_checks`: Metrics produced by monitoring models;
* `_hs_latency`: Latency, indicating how much it took to process a request;
* `_hs_error`: Error message that occurred during request processing; 
* `_hs_score`: The number of all successful _checks_ divided by the number of all _checks_; 
* `_hs_overall_score`: The amount of all successful _metric_ values \(not exceeding a specified threshold\), divided by the amount of all _metric_ values; 
* `_hs_timestamp`: Timestamp in nanoseconds, when the object was generated; 
* `_hs_year`: Year when the object was generated; 
* `_hs_month`: Month when the object was generated;
* `_hs_day`: Day when the object was generated;

Apart from the fields defined above, each object will have additional fields specific to the particular model version and its interface.

* `_hs_<field_name>_score`: The number of all successful checks calculated for this specific field divided by the total number of all checks calculated for this specific field;  
* `<field_name>`: The value of the field.

#### Raw checks object

`_hs_raw_checks` object contains all fields, for which checks have been calculated.

The example below shows, how the `_hs_raw_checks_` object can be defined.

```javascript
{
    "<field_name>": [
        ...
    ]
}
```

#### Check object

`check` object declares the check, that has been calculated for the particular field. The following members will be present in the object.

* `check`: Boolean value indicating, whether the check has been passed;
* `description`: Description of the check that has been calculated;
* `threshold`: Threshold of the check;  
* `value`: Value of the field; 
* `metricSpecId`: Metric specification ID. For each `check` object this value will be set to `null`. 

The example below shows, how the `check` object can be defined.

```javascript
{
    "check": true,
    "description": "< max",
    "threshold": 0.9321230184950273,
    "value": 0.2081205412912307,
    "metricSpecId": null
}
```

#### Metrics object

`_hs_metrics_checks` object contains all fields for which metrics have been calculated.

The example below shows how the `_hs_metrics_checks` object can be defined.

```javascript
{
    "<field_name>": {
        ...
    }
}
```

#### Metric object

`metric` object declares the metric, that has been calculated for the particular field. The following members will be present in the object.

* `check`: Boolean value indicating, whether the metric has not been fired;
* `description`: Name of the metric that has been calculated;
* `threshold`: Threshold of the metric;  
* `value`: Value of the metric; 
* `metricSpecId`: Metric specification ID.

The example below shows how the `metric` object can be defined.

```javascript
{
    "check": true, 
    "description": "string", 
    "threshold": 5.0,
    "value": 4.0,
    "metricSpecId": "bbb34c1f-13e1-4d1c-ad29-6e27c5461c37"
}
```

The example below shows a fully composed server response.

```javascript
HTTP/1.1 200 OK
Content-Type: application/json

[
    {
        "_id": "5e1717f687a34b00086f58d8",
        "in": 0.2081205412912307,
        "out": 0.5551249161117925,
        "_hs_in_score": 1.0,
        "_hs_out_score": 1.0,
        "_hs_raw_checks": {
            "in": [
                {
                    "check": true,
                    "description": "< max",
                    "threshold": 0.9321230184950273,
                    "value": 0.2081205412912307,
                    "metricSpecId": null
                },
                {
                    "check": true,
                    "description": "> min",
                    "threshold": 0.0001208467391203,
                    "value": 0.2081205412912307,
                    "metricSpecId": null
                }
            ],
            "out": [
                {
                    "check": true,
                    "description": "< max",
                    "threshold": 0.9921230184950273,
                    "value": 0.5551249161117925,
                    "metricSpecId": null
                },
                {
                    "check": true,
                    "description": "> min",
                    "threshold": 0.0201208467391203,
                    "value": 0.5551249161117925,
                    "metricSpecId": null
                }
            ],
        },
        "_hs_metric_checks": {
            "string": {
                "check": true, 
                "description": "KNN", 
                "threshold": 5.0,
                "value": 4.0,
                "metricSpecId": "bbb34c1f-13e1-4d1c-ad29-6e27c5461c37"
            },
        },
        "_hs_latency": 0.7166033601366634,
        "_hs_error": "string",
        "_hs_score": 1.0,
        "_hs_overall_score": 1.0,
        "_hs_model_version_id": 1,
        "_hs_model_name": "external-model-example",
        "_hs_model_incremental_version": 1,
        "_hs_request_id": "395ae721-5e68-46e1-8ed6-74e360c614c1",
        "_hs_timestamp": 1578571766000,
        "_hs_year": 2020,
        "_hs_month": 1,
        "_hs_day": 9
    }
]
```
