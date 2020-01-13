# Monitor external models

We can use monitoring to track behavior of external models running 
outside of the Hydrosphere platform. This article describes how to 
register an external model and how to trigger analysis over your 
requests.

## Before you start

We assume, you already have: 

- a @ref[running](../install/index.md) instance of Hydrosphere with 
the @ref[Sonar](../concepts/index.md#sonar) component enabled;
- a running external model, capable of producing predictions.

## Model registration

First, you have to register an external model. To do that you have to
submit a JSON document, defining your model. 

### Request document structure

This section describes the structure of the JSON document used to 
register external models within the platform. 

#### Top-level members

A document **must** contain the following top-level members, describing 
the interface of your model. 

- `name`: The name of the registered model. This name uniquely identifies
a collection of model versions, registered within Hydrosphere platform;
- `contract`: The interface of the registered model. This member describes
inputs and outputs of the model as well as other complementary metadata, 
such as model signature, data profile for each field.  

A document **may** contain additional top-level member, describing 
other user-specific metadata of your model. 

- `metadata`: The metadata of the registered model. The structure of 
the object is not strictly defined, the only constraint is that the 
object must have a key-value structure, where a value can only be of 
a simple data type (string, number, boolean). 

An example below shows, how a model can be defined on a top level.

```json
{  
    "name": "external-model-example",
    "metadata": {
        "architecture": "Feed-forward neural network",
        "description": "Sample external model example",
        "author": "Hydrosphere.io",
        "training-data": "s3://bucket/external-model-example/data/",
        "endpoint": "http://example.com/api/external-model/"
    },
    "contract": {
        ...
    }
}
```

#### Contract object

`contract` object appears in the document to define the interface of 
the model. The contract object **must** contain the following members:

- `modelName`: The original name of the model. Should be the same as 
the name of the registered model, defined on the level above;
- `predict`: The signature of the model. Defines the inputs and the 
outputs of the model. 

An example below shows, how `contract` object can be defined.

```json
{
    "modelName": "external-model-example",
    "predict": {
        ...
    }
}
```

#### Predict object

`predict` object describes the signature of the model. The signature 
object **must** contain the following members:

- `signatureName`: The signature of the model, used to process the 
request;
- `inputs`: A collection of fields, defining the inputs of the model. 
Each item in the collection describes a single data entry, its type, 
shape and profile. A collection **must** contain at least one item;
- `outputs`: A collection of fields, defining the outputs of the model.
Each item in the collection describes a single data entry, its type,
shape and profile. A collection **must** contain at least one item. 

An example below shows, how `predict` object can be defined.

```json
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

Items in the `inputs` / `outputs` collections are collectively called 
"fields". The field object **must** contain the following members:

- `name`: Name of the field;
- `dtype`: Data type of the field. 
- `profile`: Data profile of the field. 
- `shape`: Shape of the field.

The only *valid* options for `dtype` are:

- DT_STRING;
- DT_BOOL;
- DT_VARIANT;
- DT_HALF;
- DT_FLOAT;
- DT_DOUBLE;
- DT_INT8;
- DT_INT16;
- DT_INT32;
- DT_INT64;
- DT_UINT8;
- DT_UINT16;
- DT_UINT32;
- DT_UINT64;
- DT_QINT8;
- DT_QINT16;
- DT_QINT32;
- DT_QUINT8;
- DT_QUINT16;
- DT_COMPLEX64;
- DT_COMPLEX128;

The only *valid* options for `profile` are: 

- NONE
- NUMERICAL
- TEXT
- IMAGE

An example below shows, how a single `field` object can be defined.

```json
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

`shape` object defines the shape of the data that model is processing. 
The shape object **must** contain the following members:

- `dim`: A collection of items, describing each dimension. A collection
**may** be empty — in that case the tensor will be interpreted as a scalar
value. 
- `unknownRank`: Boolean value. Identifies, whether the defined 
shape is of the unknown rank.

An example below shows, how `shape` object can be defined.

```json
{
    "dim": [
        ...
    ],
    "unknownRank": false
}
```

#### Dim object

`dim` object defines a dimension of the field. The dim object **must** 
contain the following members:

- `size`: A size of the dimension. 
- `name`: A name of the dimension.

An example below shows, how `dim` object can be defined.

```json
{
    "size": 10,
    "name": "example"
}
```

### Registering external model

A model can be registered by sending a `POST` request to the 
`/api/v2/externalmodel` endpoint. The request **must** include a model
definition as primary data.

A request below shows an example of external model registration.

```json
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
    "contract": {
        "modelName": "external-model-example",
        "predict": {
            "signatureName": "predict",
            "inputs": [
                {
                    "name": "in",
                    "dtype": "DT_INT32",
                    "profile": "NUMERICAL",
                    "shape": {
                        "dim": [
                            {
                                "size": 10,
                                "name": "example"
                            }
                        ],
                        "unknownRank": false
                    }
                }
            ],
            "outputs": [
                {
                    "name": "out",
                    "dtype": "DT_INT32",
                    "profile": "NUMERICAL",
                    "shape": {
                        "dim": [],
                        "unknownRank": false
                    }
                }
            ]
        }
    }
}
```

As a response server will return a JSON object with complementary
metadata, identifying a registered model version. 

```json
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
    "modelContract": { 
        "modelName": "external-model-example",
        "predict": {
            "signatureName": "predict",
            "inputs": [
                {
                    "name": "in",
                    "dtype": "DT_INT32",
                    "profile": "NUMERICAL",
                    "shape": {
                        "dim": [
                            {
                                "size": 10,
                                "name": "example"
                            }
                        ],
                        "unknownRank": false
                    }
                }
            ],
            "outputs": [
                {
                    "name": "out",
                    "dtype": "DT_INT32",
                    "profile": "NUMERICAL",
                    "shape": {
                        "dim": [],
                        "unknownRank": false
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
    }
}
```

### Response document structure

The response object from external model registration request contains 
the following fields:

- `id`: Model version ID, uniquely identifying a registered model 
version within Hydrosphere platform;
- `model`: An object, representing a model collection, registered in 
Hydrosphere platform;
- `modelVersion`: Model version number in the model collection; 
- `modelContract`: Contract of the model, similar to one, defined 
in the request section above;
- `metadata`: Metadata of the model, similar to one, defined in the 
request section above;
- `created`: Timestamp, indicating when the model has been registered. 

@@@ note
Note the `id` field. It will be referred as MODEL_VERSION_ID later 
throughout the article.
@@@

#### Model object

`model` object represents a collection of model versions, registered 
in the platform. The response `model` object contains the following 
fields:

- `id`: Id of the model collection;
- `name`: Name of the model collection.

The example below shows, a sample response of the model registration.


## Data upload

In order to let Hydrosphere calculate metrics over your requests you 
have to submit the training data. You can do it by: 

- using @ref[CLI](../components/cli.md);
- using HTTP endpoint.

In each case your training data should be represented as a CSV document, 
containing fields named exactly like in the 
@ref[interface](#request-document-structure) of your model. 

@@@ note 
Currently we support uploading training data for NUMERICAL and TEXT profiles 
only. 
@@@

### Upload using CLI

Switch to the cluster, suitable for your current flow. 

```
$ hs cluster use example-cluster
Switched to cluster '{'cluster': {'server': '<hydrosphere>'}, 'name': 'example-cluster'}'
```

If you don't have a defined cluster yet, create one using the following
command. 

```
$ hs cluster add --server <hydrosphere> --name example-cluster
Cluster 'example-cluster' @ <hydrosphere> added successfully
$ hs cluster use example-cluster
```

Make sure, you have a local copy of the training data that you would like 
to submit.

```
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

- `--model-version`: A string, indicating a model version to which 
you want to submit the data. The string should be formatted in the 
following form `<model-name>:<model-version>`;
- `--filename`: Path to a filename, that you want to submit. 

@@@ note
If you already have your training data uploaded to S3, you can 
specify path to that object URI using `--s3path` parameter instead 
of `--filename`. The object behind this URI should be available to 
the Hydrosphere instance. 
@@@

```
$ hs profile push \
    --model-version external-model-example:1 \
    --filename external-model-data.csv
```

Depending on the size of you data you would have to wait till data 
will be uploaded. If you don't want to wait you can use `--async` flag.

### Upload using HTTP endpoint

If you're willing to upload your data using an HTTP endpoint, you 
would have to implement a client, which will stream your data to the 
`/monitoring/profiles/batch/<MODEL_VERSION_ID>` endpoint. 

In the code snippets below you can see how data can be uploaded 
using sample http clients. 

Python
:   @@snip [client.py](snippets/python/external-model/data-upload.py)

Java
:   @@snip [client.java](snippets/java/external-model/data-upload.java)

@@@ note
You can acquire `MODEL_VERSION_ID` by sending a GET request to 
`/model/version/<MODEL_NAME>/<MODEL_VERSION>` endpoint. Response document would 
have a similar structure, already defined @ref[above](#response-document-structure). 
@@@

## Analysis invocation

To send a request for analysis you have to use gRPC endpoint. We 
already have a [predefined](https://github.com/Hydrospheredata/hydro-serving-protos) 
ProtoBuf messages for the reference. 

1. Create an *[ExecutionMetadata](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/metadata.proto#L22)* 
message, which contains a metadata information of the model, used to 
process a given request:
2. Create a *[PredictRequest](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/api/predict.proto#L14)* 
message, which contains original request passed to the serving model 
for the prediction:
3. Create a *[PredictResponse](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/api/predict.proto#L26)* 
message, which contains inferenced output of the model: 
4. Assemble an *[ExecutionInformation](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L10)*
from the created above messages.
5. Submit ExecutionInformation proto to @ref[Sonar](../concepts/index.md#sonar) 
for analysis. Use RPC *[Analyse](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L20)*
method of the *[MonitoringService](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L19)* 
to calculate metrics.

In the code snippets below you can see how analysis can be triggered 
with sample gRPC clients. 

Python
:   @@snip [client.py](snippets/python/external-model/grpc.py)

Java
:   @@snip [client.java](snippets/java/external-model/grpc.java)

## Fetching metrics

The [analyze](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L20) 
method doesn't return anything once it was triggered. To fetch 
calculated metrics from the model version you have to make a
GET request to the `/monitoring/checks/all/<MODEL_VERSION_ID>` 
endpoint. 

A request **must** contain the following parameters: 

- `limit`: how many requests to fetch;
- `offset`: which offset to make from the beginning.

An example request is shown below.

```json
GET /monitoring/checks/all/1?limit=1&offset=0 HTTP/1.1
Accept: application/json
```

Calculated metrics have a dynamic structure which is dependant on 
the model interface.

### Response object structure

Response object contains the original data submitted for prediction, 
model's response, calculated metrics and other supplementary metadata. 
Every field produced by Hydrosphere is prefixed with `_hs_` char. 

- `_id`: ID of the request, generated internally by Hydrosphere; 
- `_hs_request_id`: ID of the request, specified by user;
- `_hs_model_name`: Name of the model, which processed a request;
- `_hs_model_incremental_version`: Version of the model, which processed 
a request; 
- `_hs_model_version_id`: ID of the model version, which processed a 
request;
- `_hs_raw_checks`: Raw checks calculated by Hydrosphere based on the 
training data;
- `_hs_metric_checks`: Metrics produced by monitoring models;
- `_hs_latency`: Latency, indicating how much it took to process a 
request;
- `_hs_error`: Error message, which occurred during request processing; 
- `_hs_score`: The amount of all successful *checks* divided by the amount 
of all *checks*; 
- `_hs_overall_score`: The amount of all successful *metric* values (not 
exceeded a specified threshold), divided by the amount of all *metric* 
values; 
- `_hs_timestamp`: Timestamp in nanoseconds, when this object has been 
generated; 
- `_hs_year`: Year, when this object has been generated; 
- `_hs_month`: Month, when this object has been generated;
- `_hs_day`: Day, when this object has been generated;

Apart from the fields defined above, each object will have additional 
fields specific to the particular model version and its interface. 

- `_hs_<field_name>_score`: The amount of all successful checks calculated
for this specific field divided by the amount of all checks calculated for 
this specific field;  
- `<field_name>`: The value of the field.

#### Raw checks object

`_hs_raw_checks` object contains all fields, for which checks have been 
calculated. 

An example below shows, how `_hs_raw_checks_` object can be defined.

```json
{
    "<field_name>": [
        ...
    ]
}
```

#### Check object

`check` object declares the check, that has been calculated for the 
particular field. The following members will be present in the object. 

- `check`: Boolean value indicating, whether the check has been passed;
- `description`: Description of the check, which has been calculated;
- `threshold`: Threshold of the check;  
- `value`: Value of the field; 
- `metricSpecId`: Metric specification ID. For each `check` object this 
value will be set to `null`. 

An example below shows, how `check` object can be defined.

```json
{
    "check": true,
    "description": "< max",
    "threshold": 0.9321230184950273,
    "value": 0.2081205412912307,
    "metricSpecId": null
}
```

#### Metrics object

`_hs_metrics_checks` object contains all fields, for which metrics have 
been calculated. 

An example below shows, how `_hs_metrics_checks` object can be defined.

```json
{
    "<field_name>": {
        ...
    }
}
```

#### Metric object

`metric` object declares the metric, that has been calculated for the 
particular field. The following members will be present in the object. 

- `check`: Boolean value indicating, whether the metric hasn't been 
fired;
- `description`: Name of the metric, which has been calculated;
- `threshold`: Threshold of the metric;  
- `value`: Value of the metric; 
- `metricSpecId`: Metric specification ID.

An example below shows, how `metric` object can be defined.

```json
{
    "check": true, 
    "description": "string", 
    "threshold": 5.0,
    "value": 4.0,
    "metricSpecId": "bbb34c1f-13e1-4d1c-ad29-6e27c5461c37"
}
```

An example of a fully composed server response is shown below. 

```json
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
