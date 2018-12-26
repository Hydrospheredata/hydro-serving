---
layout: docs
title:  "Invoke Applications"
permalink: 'invoke-applications.html'
---

# Invoke Applications

Applications can be invoked in three following ways. 

### Test UI call

You can perform a test request to the application from UI interface. Open a desired application and press `Test` button. Internally it will generate dummy input data from model's contract and send an HTTP-request to API endpoint. 

### HTTP API call

You can reach your application with an HTTP-request. Send a `POST` request to `http://<host>/gateway/applications/{applicationName}/{applicationSignature}`. 

Note, when you create a `pipeline` application, ML Lambda internally infers a contract. It performs validation that every stage is compatible with it's siblings, and creates a contract with the same signature name, as the application name. `singular` applications by default use their explicitly defined signatures.

### gRPC API call

You can define a gRPC client on your side, establish insecure connection with `http://<host>` and make a call to `Predict` method. We provide an example Python client. 

```python
import grpc 
import hydro_serving_grpc as hs  # pip install hydro-serving-grpc

# connect to your ML Lamba instance
channel = grpc.insecure_channel("<host>")
stub = hs.PredictionServiceStub(channel)

# 1. define a model, that you'll use
model_spec = hs.ModelSpec(name="model", signature_name="infer")

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
