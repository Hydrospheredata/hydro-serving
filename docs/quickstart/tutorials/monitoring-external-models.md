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
* `contract`: the interface of the registered model. This member describes inputs and outputs of the model, as well as other complementary metadata, such as model signature, and data profile for each field.  

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
    "contract": {
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

#### Contract object

The`contract` object appears in the document to define the interface of the model. The contract object **must** contain the following members:

* `modelName`: the original name of the model. It should be the same as the name of the registered model, defined on the level above;
* `predict`: the signature of the model. It defines the inputs and the outputs of the model. 

The example below shows how a `contract` object can be defined.

```javascript
{
    "modelName": "external-model-example",
    "predict": {
        ...
    }
}
```

#### Predict object

`predict` object describes the signature of the model. The signature object **must** contain the following members:

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

* `dim`: An array of dimensions. A collection **may** be empty — in that case, the tensor will be interpreted as a scalar value. 

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
    "contract": {
        "modelName": "external-model-example",
        "predict": {
            "signatureName": "predict",
            "inputs": [
                {
                    "name": "in",
                    "dtype": "DT_DOUBLE",
                    "profile": "NUMERICAL",
                    "shape": {
                        "dim": [],
                        "unknownRank": false
                    }
                }
            ],
            "outputs": [
                {
                    "name": "out",
                    "dtype": "DT_DOUBLE",
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

As a response, the server will return a JSON object with complementary metadata, identifying a registered model version.

### Response document structure

The response object from the external model registration request contains the following fields:

* `id`: Model version ID, uniquely identifying a registered model version within Hydrosphere platform;
* `model`: An object, representing a model collection, registered in Hydrosphere platform;
* `modelVersion`: Model version number in the model collection; 
* `modelContract`: Contract of the model, similar to the one defined in the request section above;
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
    "modelContract": { 
        "modelName": "external-model-example",
        "predict": {
            "signatureName": "predict",
            "inputs": [
                {
                    "name": "in",
                    "dtype": "DT_DOUBLE",
                    "profile": "NUMERICAL",
                    "shape": {
                        "dim": [],
                        "unknownRank": false
                    }
                }
            ],
            "outputs": [
                {
                    "name": "out",
                    "dtype": "DT_DOUBLE",
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

In the code snippets below you can see how data can be uploaded using sample HTTP clients.

{% tabs %}
{% tab title="Python" %}
```python
from argparse import ArgumentParser
from urllib.parse import urljoin

import requests


def read_in_chunks(filename, chunk_size=1024):
    """ Generator to read a file peace by peace. """
    with open(filename, "rb") as file:
        while True:
            data = file.read(chunk_size)
            if not data:
                break
            yield data


if __name__ == "__main__": 
    parser = ArgumentParser()
    parser.add_argument("--hydrosphere", type=str, required=True)
    parser.add_argument("--model-version-id", type=int, required=True)
    parser.add_argument("--filename", required=True)
    parser.add_argument("--chunk-size", default=1024)
    args, unknown = parser.parse_known_args()
    if unknown:
        print("Parsed unknown arguments: %s", unknown)

    endpoint_uri = "/monitoring/profiles/batch/{}".format(args.model_version_id)
    endpoint_uri = urljoin(args.hydrosphere, endpoint_uri) 

    gen = read_in_chunks(args.filename, chunk_size=args.chunk_size)
    response = requests.post(endpoint_uri, data=gen, stream=True)
    if response.status_code != 200:
        print("Got error:", response.text)
    else:
        print("Uploaded data:", response.text)
```
{% endtab %}

{% tab title="Java" %}
```java
import com.google.common.io.Files;

import java.io.*;
import java.net.*;


public class DataUploader {
    private String endpointUrl = "/monitoring/profiles/batch/";

    private String composeUrl(String base, long modelVersionId) throws java.net.URISyntaxException {
        return new URI(base).resolve(this.endpointUrl + modelVersionId).toString();
    }

    public int upload(String baseUrl, String filePath, long modelVersionId) throws Exception {
        String composedUrl = this.composeUrl(baseUrl, modelVersionId);
        HttpURLConnection connection = (HttpURLConnection) new URL(composedUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(4096);

        OutputStream output = connection.getOutputStream();
        Files.copy(new File(filePath), output);
        output.flush();

        return connection.getResponseCode();
    }

    public static void main(String[] args) throws Exception {
        DataUploader dataUploader = new DataUploader();
        int responseCode = dataUploader.upload(
            "http://<hydrosphere>/", "/path/to/data.csv", 1);
        System.out.println(responseCode);
    }
}
```
{% endtab %}
{% endtabs %}

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

To send a request for analysis you have to use gRPC endpoint. We have already [predefined](https://github.com/Hydrospheredata/hydro-serving-protos) ProtoBuf messages for the reference.

1. Create an [_ExecutionMetadata_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/metadata.proto#L22) message that contains metadata information of the model, used to process a given request:
2. Create a [_PredictRequest_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/api/predict.proto#L14) message that contains the original request passed to the serving model for the prediction:
3. Create a [_PredictResponse_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/api/predict.proto#L26) message that contains inferenced output of the model: 
4. Assemble an [_ExecutionInformation_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L10) from the above-created messages.
5. Submit ExecutionInformation proto to Sonar for analysis. Use the RPC [_Analyse_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L20) method of the [_MonitoringService_](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L19) to calculate metrics.

In the code snippets below you can see how analysis can be triggered with sample gRPC clients.

{% tabs %}
{% tab title="Python" %}
```python
import uuid
import grpc
import random
import hydro_serving_grpc as hs

use_ssl_connection = True
if use_ssl_connection:
    creds = grpc.ssl_channel_credentials()
    channel = grpc.secure_channel(HYDROSPHERE_INSTANCE_GRPC_URI, credentials=creds)
else:
    channel = grpc.insecure_channel(HYDROSPHERE_INSTANCE_GRPC_URI) 
monitoring_stub = hs.MonitoringServiceStub(channel)

# 1. Create an ExecutionMetadata message. ExecutionMetadata is used to define, 
# which model, registered within Hydrosphere platform, was used to process a 
# given request.
trace_id = str(uuid.uuid4())  # uuid used as an example
execution_metadata_proto = hs.ExecutionMetadata(
    model_name="external-model-example",
    modelVersion_id=2,
    model_version=3,
    signature_name="predict",
    request_id=trace_id,
    latency=0.014,
)

# 2. Create a PredictRequest message. PredictRequest is used to define the data 
# passed to the model for inference.
predict_request_proto = hs.PredictRequest(
    model_spec=hs.ModelSpec(
        name="external-model-example",
        signature_name="predict", 
    ),
    inputs={
        "in": hs.TensorProto(
            dtype=hs.DT_DOUBLE, 
            double_val=[random.random()], 
            tensor_shape=hs.TensorShapeProto()
        ),
    }, 
)

# 3. Create a PredictResponse message. PredictResponse is used to define the 
# outputs of the model inference.
predict_response_proto = hs.PredictResponse(
    outputs={
        "out": hs.TensorProto(
            dtype=hs.DT_DOUBLE, 
            double_val=[random.random()], 
            tensor_shape=hs.TensorShapeProto()
        ),
    },
)

# 4. Create an ExecutionInformation message. ExecutionInformation contains all 
# request data and all auxiliary information about request execution, required 
# to calculate metrics.
execution_information_proto = hs.ExecutionInformation(
    request=predict_request_proto,
    response=predict_response_proto,
    metadata=execution_metadata_proto,
)

# 5. Use RPC method Analyse of the MonitoringService to calculate metrics
monitoring_stub.Analyze(execution_information_proto)
```
{% endtab %}

{% tab title="Java" %}
```java
import io.hydrosphere.serving.monitoring.MonitoringServiceGrpc;
import io.hydrosphere.serving.monitoring.MonitoringServiceGrpc.MonitoringServiceBlockingStub;
import io.hydrosphere.serving.monitoring.Metadata.ExecutionMetadata;
import io.hydrosphere.serving.monitoring.Api.ExecutionInformation;
import io.hydrosphere.serving.tensorflow.api.Predict.PredictRequest;
import io.hydrosphere.serving.tensorflow.api.Predict.PredictResponse;
import io.hydrosphere.serving.tensorflow.TensorProto;
import io.hydrosphere.serving.tensorflow.TensorShapeProto;
import io.hydrosphere.serving.tensorflow.DataType;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class HydrosphereClient {

    private final String modelName;         // Actual model name, registered within Hydrosphere platform
    private final long modelVersion;        // Model version of the registered model within Hydrosphere platform
    private final long modelVersionId;      // Model version Id, which uniquely identifies any model within Hydrosphere platform
    private final ManagedChannel channel;
    private final MonitoringServiceBlockingStub blockingStub;

    public HydrosphereClient(String target, String modelName, long modelVersion, long modelVersionId) {
        this(ManagedChannelBuilder.forTarget(target).build(), modelName, modelVersion, modelVersionId);
    }

    HydrosphereClient(ManagedChannel channel, String modelName, long modelVersion, long modelVersionId) {
        this.channel = channel;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.modelVersionId = modelVersionId;
        this.blockingStub = MonitoringServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private double getLatency() {
        /*
        Random value is used as an example. Acquire the actual latency
        value, during which a model processed a request.
        */
        return new Random().nextDouble();
    }

    private String getTraceId() {
        /*
        UUID used as an example. Use this value to track down your
        requests within Hydrosphere platform.
        */
        return UUID.randomUUID().toString();
    }

    private TensorProto generateDoubleTensorProto() {
        /*
        Helper method generating TensorProto object with random double values.
        */
        return TensorProto.newBuilder()
                .addDoubleVal(new Random().nextDouble())
                .setDtype(DataType.DT_DOUBLE)
                .setTensorShape(TensorShapeProto.newBuilder().build())  // Empty TensorShape indicates scalar shape
                .build();
    }

    private PredictRequest generatePredictRequest() {
        /*
        PredictRequest is used to define the data passed to the model for inference.
        */
        return PredictRequest.newBuilder()
                .putInputs("in", this.generateDoubleTensorProto()).build();
    }

    private PredictResponse generatePredictResponse() {
        /*
        PredictResponse is used to define the outputs of the model inference.
        */
        return PredictResponse.newBuilder()
                .putOutputs("out", this.generateDoubleTensorProto()).build();
    }

    private ExecutionMetadata generateExecutionMetadata() {
        /*
        ExecutionMetadata is used to define, which model, registered within Hydrosphere
        platform, was used to process a given request.
        */
        return ExecutionMetadata.newBuilder()
                .setModelName(this.modelName)
                .setModelVersion(this.modelVersion)
                .setModelVersionId(this.modelVersionId)
                .setSignatureName("predict")                // Use default signature of the model
                .setLatency(this.getLatency())              // Get latency for a given request
                .setRequestId(this.getTraceId())            // Get traceId to track a given request within Hydrosphere platform
                .build();
    }

    public ExecutionInformation generateExecutionInformation() {
        /*
        ExecutionInformation contains all request data and all auxiliary information
        about request execution, required to calculate metrics.
        */
        return ExecutionInformation.newBuilder()
                .setRequest(this.generatePredictRequest())
                .setResponse(this.generatePredictResponse())
                .setMetadata(this.generateExecutionMetadata())
                .build();
    }

    public void analyzeExecution(ExecutionInformation executionInformation) {
        /*
        The actual use of RPC method Analyse of the MonitoringService to invoke
        metrics calculation.
        */
        this.blockingStub.analyze(executionInformation);
    }

    public static void main(String[] args) throws Exception {
        /*
        Test client functionality by sending randomly generated data for analysis.
        */
        HydrosphereClient client = new HydrosphereClient("<hydrosphere>", "external-model-example", 1, 1);
        try {
            int requestAmount = 10;
            System.out.printf("Analysing %d randomly generated samples\n", requestAmount);
            for (int i = 0; i < requestAmount; i++) {
                ExecutionInformation executionInformation = client.generateExecutionInformation();
                client.analyzeExecution(executionInformation);
            }
        } finally {
            System.out.println("Shutting down client");
            client.shutdown();
        }
    }
}
```
{% endtab %}
{% endtabs %}

## Metrics retrieval

Once triggered, the [analyze](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/monitoring/api.proto#L20) method does not return anything. To fetch calculated metrics from the model version, you have to make a GET request to the `/monitoring/checks/all/<MODEL_VERSION_ID>` endpoint.

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

