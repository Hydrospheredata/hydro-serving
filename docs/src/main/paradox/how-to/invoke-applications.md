# Invoke applications

Inferencing applications can be achieved using any of the methods described below.

## Hydrosphere UI 

To send a sample request using Hydrosphere UI, open a desired application and press the `Test` button at the upper right corner. We will generate dummy inputs based on your model's contract and send an HTTP request to the model's endpoint. 

## HTTP API

To send an HTTP request, you should send a `POST` request to the `/gateway/application/<applicationName>` endpoint with the JSON body containing your request data, composed with respect to the model's contract. 

## gRPC API

To send a gRPC request you need to create a specific client. 

Python
:   @@snip[client.py](snippets/python/invoke-applications/grpc.py)

Java
:   @@snip[client.java](snippets/java/invoke-applications/grpc.java)

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