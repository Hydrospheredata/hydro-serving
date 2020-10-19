---
description: >-
  Inferencing applications can be achieved using any of the methods described
  below.
---

# Invoke applications

## Hydrosphere UI

To send a sample request using Hydrosphere UI, open the desired application, and press the **Test** button at the upper right corner. We will generate dummy inputs based on your model's contract and send an HTTP request to the model's endpoint.

{% api-method method="post" host="" path="/gateway/application/<application\_name>" %}
{% api-method-summary %}
HTTP Inference
{% endapi-method-summary %}

{% api-method-description %}
To send an HTTP request, you should send a POST request to the **/gateway/application/&lt;applicationName&gt;** endpoint with the JSON body containing your request data, composed with respect to the model's contract.
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}
{% api-method-path-parameters %}
{% api-method-parameter name="application\_name" type="string" required=true %}
Name of the application
{% endapi-method-parameter %}
{% endapi-method-path-parameters %}

{% api-method-body-parameters %}
{% api-method-parameter required=true type="object" %}
Request data, composed with respect to the model's contract.
{% endapi-method-parameter %}
{% endapi-method-body-parameters %}
{% endapi-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}

{% endapi-method-response-example-description %}

```

```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

## gRPC

To send a gRPC request you need to create a specific client.

{% tabs %}
{% tab title="Python" %}
```python
import grpc 
import hydro_serving_grpc as hs  # pip install hydro-serving-grpc

# connect to your ML Lamba instance
channel = grpc.insecure_channel("<host>")
stub = hs.PredictionServiceStub(channel)

# 1. define a model, that you'll use
model_spec = hs.ModelSpec(name="model")

# 2. define tensor_shape for Tensor instance
tensor_shape = hs.TensorShapeProto(
    dim=[hs.TensorShapeProto.Dim(size=-1), hs.TensorShapeProto.Dim(size=2)])

# 3. define tensor with needed data
tensor = hs.TensorProto(dtype=hs.DT_DOUBLE, tensor_shape=tensor_shape, double_val=[1,1,1,1])

# 4. create PredictRequest instance
request = hs.PredictRequest(model_spec=model_spec, inputs={"x": tensor})

# call Predict method
result = stub.Predict(request)
```
{% endtab %}

{% tab title="Java" %}
```java
import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.hydrosphere.serving.tensorflow.DataType;
import io.hydrosphere.serving.tensorflow.TensorProto;
import io.hydrosphere.serving.tensorflow.TensorShapeProto;
import io.hydrosphere.serving.tensorflow.api.Model;
import io.hydrosphere.serving.tensorflow.api.Predict;
import io.hydrosphere.serving.tensorflow.api.PredictionServiceGrpc;

import java.util.Random;

public class HydrosphereClient {

    private final String modelName;         // Actual model name, registered within Hydrosphere platform
    private final Int64Value modelVersion;  // Model version of the registered model within Hydrosphere platform
    private final ManagedChannel channel;
    private final PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub;

    public HydrosphereClient2(String target, String modelName, long modelVersion) {
        this(ManagedChannelBuilder.forTarget(target).build(), modelName, modelVersion);
    }

    HydrosphereClient2(ManagedChannel channel, String modelName, long modelVersion) {
        this.channel = channel;
        this.modelName = modelName;
        this.modelVersion = Int64Value.newBuilder().setValue(modelVersion).build();
        this.blockingStub = PredictionServiceGrpc.newBlockingStub(this.channel);
    }

    private Model.ModelSpec getModelSpec() {
        /*
        Helper method to generate ModelSpec.
         */
        return Model.ModelSpec.newBuilder()
                .setName(this.modelName)
                .setVersion(this.modelVersion)
                .build();
    }

    private TensorProto generateDoubleTensorProto() {
        /*
        Helper method generating random TensorProto object for double values.
        */
        return TensorProto.newBuilder()
                .addDoubleVal(new Random().nextDouble())
                .setDtype(DataType.DT_DOUBLE)
                .setTensorShape(TensorShapeProto.newBuilder().build())  // Empty TensorShape indicates scalar shape
                .build();
    }

    public Predict.PredictRequest generatePredictRequest() {
        /*
        PredictRequest is used to define the data passed to the model for inference.
        */
        return Predict.PredictRequest.newBuilder()
                .putInputs("in", this.generateDoubleTensorProto())
                .setModelSpec(this.getModelSpec())
                .build();
    }


    public Predict.PredictResponse predict(Predict.PredictRequest request) {
        /*
        The actual use of RPC method Predict of the PredictionService to invoke prediction.
        */
        return this.blockingStub.predict(request);
    }

    public static void main(String[] args) throws Exception {
        HydrosphereClient client = new HydrosphereClient("<host>", "example", 2);
        Predict.PredictRequest request = client.generatePredictRequest();
        Predict.PredictResponse response = client.predict(request);
        System.out.println(response);
    }
}
```
{% endtab %}
{% endtabs %}

## Python SDK

You can learn more about our Python SDK [here](https://hydrospheredata.github.io/hydro-serving-sdk/index.html).

```python
import hydrosdk as hs

hs_cluster = hs.Cluster(http_address='{HTTP_CLUSTER_ADDRESS}',
                         grpc_address='{GRPC_CLUSTER_ADDRESS}',)

app = hs.Application.find(hs_cluster, "{APP_NAME}")

predictor = adult_servable.predictor()

data  = ...  # your data
predictor.predict(data)
```

