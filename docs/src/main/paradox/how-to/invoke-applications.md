# Invoke Applications

You can use on the following APIs to send prediction requests to your 
application:

## Web-UI `Test` button

You can send a test request to the application from UI interface. Just 
open an application and press `Test` button. We will generate dummy input 
data based on your model's contract and send an HTTP-request to API 
endpoint. 

## HTTP API

You can reach your application with an HTTP-request. Send a `POST` 
request to `http://<host>/gateway/application/{applicationName}`. 

## GRPC API

In order to send a prediction GRPC request you need to create a specific 
client. Gateway service exposes a `PredictionService` endpoint, so you 
can use it's client.

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
