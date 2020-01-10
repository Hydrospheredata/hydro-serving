# Monitor external models

We can use monitoring to track behavior of external models running 
outside of the Hydrosphere platform. This article describes how to 
register an external model and how to trigger monitoring to analyse 
requests.

## Before you start

We assume, you already have: 

- a @ref[running](../install/index.md) instance of Hydrosphere with 
the @ref[Sonar](../concepts/index.md#sonar) component enabled;
- a configured @ref[CLI](../components/cli.md) tool;
- a running external model, capable of producing predictions on 
incoming requests.

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
inputs and outputs of the model, as well as other complementary metadata, 
such as model signature and data profile for each field.  

A document **may** contain additional top-level member, describing 
auxiliary metadata of your model. 

- `metadata`: The metadata of the registered model. The structure of 
the document is not strictly defined, the only constraint is that the 
document must have a key-value structure, where a value can only be of 
a simple data type (string, number, boolean). 

The example below shows, how a model can be defined on a top level.

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
the model. Contract object **must** contain the following members:

- `modelName`: The original name of the model. Should be the same as 
the name of the registered model, defined on the level above;
- `predict`: The signature of the model. Defines the inputs and the 
outputs of the model. 

The example below shows, how `contract` object can be defined.

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

The example below shows, how `predict` object can be defined.

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

The example below shows, how a single `field` object can be defined.

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
**may** be empty — in that case the data will be interpreted as scalar. 
- `unknownRank`: Boolean value. Identifies, whether the defined 
shape is of the unknown rank.

The example below shows, how `shape` object can be defined.

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

The example below shows, how `dim` object can be defined.

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

```sh
POST /api/v2/externalmodel
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

### Response document structure

Response object of the external model registration would return a 
JSON object, which contains the following fields:

- `id`: Model version ID, uniquely identifying a registered model 
version within Hydrosphere platform;
- `model`: Model collection, registered in Hydrosphere platform; 
- `modelVersion`: Model version number in the model collection; 
- `modelContract`: Contract of the model, similar to one, defined 
in the request section above;
- `metadata`: Metadata of the model, similar to one, defined in the 
request section above;
- `created`: Timestamp, indicating when the model has been registered. 

#### Model object

`model` object represents a collection of model versions, registered 
in the platform. The response `model` object contains the following 
field:

- `id`: Id of the model collection;
- `name`: Name of the model collection.

The example below shows, a sample response of the model registration.

```json
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

## Data upload

In order to let Hydrosphere calculate metrics over your requests you 
have to submit the training data. You can do it by: 

- using @ref[CLI](../components/cli.md) to submit the training data;
- using HTTP endpoint to submit the training data.

In each case your training data should be represented as a CSV document, 
containing fields named exactly alike as in the interface of your model. 

@@@ note 
Currently we support training data for NUMERICAL and TEXT profiles only. 
@@@

### Upload using CLI

Switch to the cluster, suitable for your current flow. 

```
$ hs cluster use example-cluster
Switched to cluster '{'cluster': {'server': '<hydrosphere>'}, 'name': 'example-cluster'}'
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

- `--model-version`: A string, indicating a model version to which you 
want to submit the data. The string should be formatted in the following 
form `<model-name>:<model-version>`;
- `--filename`: Path to a filename, that you want to submit. 

@@@ note
`--filename` can be substituted with the `--s3path`, in which 
case you would have to provide S3 URI to your training data. The object 
behind this URI should be available to the Hydrosphere instance. 
@@@

```
$ hs profile push \
    --model-version external-model-example:1 \
    --filename external-model-data.csv
```

Depending on the size of you data you would have to wait, till data will 
be uploaded. If you don't want to wait until the upload succeeds, you can 
use `--async` flag.

### Upload using HTTP endpoint

## Analysis invocation

To send a request for analysis you have to use a gRPC endpoint. 

1. Create an *[ExecutionMetadata](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/metadata.proto#L22)* 
message, which contains a metadata information of the model, used to 
process a given request:
2. Create a *[PredictRequest](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/api/predict.proto#L14)* 
message, which contains original request passed to the serving model 
for the prediction:
3. Create a *[PredictResponse](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/api/predict.proto#L26)* 
message, which contains inferenced output of the model: 
4. Once you have all of these messages, you can create an 
*[ExecutionInformation](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L10)* 
message, which is used by Sonar to compute metrics. 
5. Correctly assembled *[ExecutionInformation](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L10)* 
message can be used to perform analysis by the Sonar service. Use RPC 
*[Analyse](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L20)* 
method of the *[MonitoringService](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L19)* 
to calculate metrics.

In the code snippets below you can see how analysis can be triggered with 
sample gRPC clients. 

Python
:   @@snip [client.py](snippets/python-external-model-grpc.py)

Java
:   @@snip [client.java](snippets/java-external-model-grpc.java)

## Retrieving metrics
