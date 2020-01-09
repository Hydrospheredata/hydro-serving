# Monitor external models

We can use monitoring to track behavior of external models running 
outside of Hydrosphere platform. This article describes how to register 
an external model and how to trigger monitoring to analyse requests.

To register an external model in Hydrosphere, make a POST request
to the platform:

```sh
curl -X POST --header 'Content-Type: application/json' \
    --header 'Accept: application/json' \
    -d '{
            "name": "external-model-example",
            "contract": {
                "modelName": "external-model-example",
                "predict": {
                    "signatureName": "predict",
                    "inputs": [
                        {
                            "name": "in",
                            "dtype": "DT_DOUBLE",
                            "shape": {
                                "dim": [
                                    {
                                        "size": 1,
                                        "name": "example"
                                    }
                                ],
                                "unknownRank": false
                            },
                            "profile": "TEXT"
                        }
                    ],
                    "outputs": [
                        {
                            "name": "out",
                            "dtype": "DT_DOUBLE",
                            "shape": {
                                "dim": [
                                    {
                                        "size": 1,
                                        "name": "example"
                                    }
                                ],
                                "unknownRank": false
                            },
                            "profile": "NUMERICAL"
                        }
                    ]
                }
            },
            "metadata": {
                "description": "External model example"
            }
        }' 'http://<hydrosphere>/api/v2/externalmodel'
```

In the response you will receive metadata information about a registered 
model. 

```json
{
  "model": {
    "id": 1,
    "name": "external-model-example"
  },
  "modelContract": {...},
  "id": 2,
  "metadata": {},
  "modelVersion": 3,
  "created": "2019-12-04T13:29:04.442Z"
}
```

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