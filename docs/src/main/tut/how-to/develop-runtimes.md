---
layout: docs
title:  "Develop Runtimes"
permalink: 'develop-runtimes.html'
---

# Develop Runtimes

Sometimes you have to use technology that we are not supporting yet or you need more flexibility and you want to implement your own runtime. It may seem frightening at first glance, but it's actually not that difficult. Serving is designed to abstract it's guts from model users and runtime developers. The key things you have to know to write your own runtime are: 

* Knowing how to implement a predefined gRPC service for a dedicated language;
* Understanding our contracts' protobufs to describe entry points, such as inputs and outputs;
* Knowing how to create your own docker image and publish it to an open registry.


## Generate GRPC code

There are different approaches on how to generate client and server gRPC code on [different languages](https://grpc.io/docs/). Let's have a look on how to do that on Python.

First, let's clone our [protocols](https://github.com/Hydrospheredata/hydro-serving-protos) and prepare a folder for generated code.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-protos
$ mkdir runtime
```

For generating gRPC code we need additional packages. 

```sh
$ pip install grpcio-tools googleapis-common-protos
```

Runtimes only depend on `contracts` and `tf`, so we're generating only them.

```sh
$ python -m grpc_tools.protoc --proto_path=./hydro-serving-protos/src/ --python_out=./runtime/ --grpc_python_out=./runtime/ $(find ./hydro-serving-protos/src/hydro_serving_grpc/contract/ -type f -name '*.proto')
$ python -m grpc_tools.protoc --proto_path=./hydro-serving-protos/src/ --python_out=./runtime/ --grpc_python_out=./runtime/ $(find ./hydro-serving-protos/src/hydro_serving_grpc/tf/ -type f -name '*.proto')
```

For convinience we can also add `__init__.py` files to the generated directories. 

```sh
$ cd runtime
$ find ./hydro_serving_grpc -type d -exec touch {}/__init__.py \;
```

The structure of the `runtime` now should be as following:

```sh
runtime
└── hydro_serving_grpc
    ├── __init__.py
    ├── contract
    │   ├── __init__.py
    │   ├── model_contract_pb2.py
    │   ├── model_contract_pb2_grpc.py
    │   ├── model_field_pb2.py
    │   ├── model_field_pb2_grpc.py
    │   ├── model_signature_pb2.py
    │   └── model_signature_pb2_grpc.py
    └── tf
        ├── __init__.py
        ├── api
        │   ├── __init__.py
        │   ├── model_pb2.py
        │   ├── model_pb2_grpc.py
        │   ├── predict_pb2.py
        │   ├── predict_pb2_grpc.py
        │   ├── prediction_service_pb2.py
        │   └── prediction_service_pb2_grpc.py
        ├── tensor_pb2.py
        ├── tensor_pb2_grpc.py
        ├── tensor_shape_pb2.py
        ├── tensor_shape_pb2_grpc.py
        ├── types_pb2.py
        └── types_pb2_grpc.py
```

## Implement Service

Now, that we have everything set up, let's actually implement runtime.

`runtime.py`
```python
from hydro_serving_grpc.tf.api.predict_pb2 import PredictRequest, PredictResponse
from hydro_serving_grpc.tf.api.prediction_service_pb2_grpc import PredictionServiceServicer, add_PredictionServiceServicer_to_server
from hydro_serving_grpc.tf.types_pb2 import *
from hydro_serving_grpc.tf.tensor_pb2 import TensorProto
from hydro_serving_grpc.contract.model_contract_pb2 import ModelContract
from concurrent import futures

import os
import time
import grpc
import logging
import importlib


class RuntimeService(PredictionServiceServicer):
    def __init__(self, model_path, contract):
        self.contract = contract
        self.model_path = model_path
        self.logger = logging.getLogger(self.__class__.__name__)

    def Predict(self, request, context):
        self.logger.info(f"Received inference request: {request}")
        
        selected_signature = None
        for signature in self.contract.signatures:
            if signature.signature_name == request.model_spec.signature_name:
                selected_signature = signature

        if selected_signature is None:
          context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
          context.set_details(f'{request.model_spec.signature_name} is not present in the model')
          return PredictResponse()

        module = importlib.import_module("func_main")
        executable = getattr(module, selected_signature.signature_name)
        result = executable(**request.inputs)

        if not isinstance(result, hs.PredictResponse):
            self.logger.warning(f"Type of a result ({result}) is not `PredictResponse`")
            context.set_code(grpc.StatusCode.OUT_OF_RANGE)
            context.set_details(f"Type of a result ({result}) is not `PredictResponse`")
            return PredictResponse()
        return result


class RuntimeManager:
    def __init__(self, model_path, port):
        self.logger = logging.getLogger(self.__class__.__name__)
        self.port = port
        self.model_path = model_path
        self.server = None
        
        with open(os.path.join(model_path, 'contract.protobin')) as file:
            contract = ModelContract.ParseFromString(file.read())
        self.servicer = RuntimeService(os.path.join(self.model_path, 'files'), contract)

    def start(self):
        self.logger.info(f"Starting PythonRuntime at {self.port}")
        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        add_PredictionServiceServicer_to_server(self.servicer, self.server)
        self.server.add_insecure_port(f'[::]:{self.port}')
        self.server.start()

    def stop(self, code=0):
        self.logger.info(f"Stopping PythonRuntime at {self.port}")
        self.server.stop(code)
```

Let's quickly review, what we have here. `RuntimeManager` simply manages our service, i.e. starts it, stops it, holds all necessary data. `RuntimeService` is the service that actually implements `Predict(PredictRequest)`.

The model will be stored inside the `/model` directory in the docker container. The structure of `/model` is following: 

```sh
model
├── contract.protobin
└── files
    ├── ...
    └── ...
```

`contract.protobin` is a processed by `manager` binary representation of the [ModelContract](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_contract.proto) message. `files` directory contains all files of your model.

To run the service let's create another file.

`main.py`
```python
from runtime import RuntimeManager

import os
import time
import logging

logging.basicConfig(level=logging.INFO)

if __name__ == '__main__':
    runtime = RuntimeManager('/model', port=int(os.getenv('APP_PORT', "9090")))
    runtime.start()

    try:
        while True:
            time.sleep(60 * 60 * 24)
    except KeyboardInterrupt:
        runtime.stop()
```

## Publish Runtime

Before we can use the runtime, we have to wrap it up into docker image.

Add requirements for installing dependencies.

`requirements.txt`
```
grpcio==1.12.1 
googleapis-common-protos==1.5.3 
``` 
Add Dockerfile to wrap up our image.

`Dockerfile`
```docker
FROM python:3.6.5 

ADD . /app
RUN pip install -r /app/requirements.txt

ENV APP_PORT=9090

VOLUME /model 
WORKDIR /app

CMD ["python", "main.py"]
```

`APP_PORT` is an environment variable that is used by Serving. When Serving invokes `Predict` method, it does it via defined port.

The structure of the `runtime` folder now should look like this:

```sh
runtime
├── Dockerfile
├── hydro_serving_grpc
│   ├── __init__.py
│   ├── contract
│   │   ├── __init__.py
│   │   ├── model_contract_pb2.py
│   │   ├── model_contract_pb2_grpc.py
│   │   ├── model_field_pb2.py
│   │   ├── model_field_pb2_grpc.py
│   │   ├── model_signature_pb2.py
│   │   └── model_signature_pb2_grpc.py
│   └── tf
│       ├── __init__.py
│       ├── api
│       │   ├── __init__.py
│       │   ├── model_pb2.py
│       │   ├── model_pb2_grpc.py
│       │   ├── predict_pb2.py
│       │   ├── predict_pb2_grpc.py
│       │   ├── prediction_service_pb2.py
│       │   └── prediction_service_pb2_grpc.py
│       ├── tensor_pb2.py
│       ├── tensor_pb2_grpc.py
│       ├── tensor_shape_pb2.py
│       ├── tensor_shape_pb2_grpc.py
│       ├── types_pb2.py
│       └── types_pb2_grpc.py
├── main.py
├── requirements.txt
└── runtime.py
```

To let Serving see your runtimes, you have to publish it to the Docker Hub or your private Docker Registry. Here for simplicity we will publish it to the Docker Hub. 

```sh
$ docker build -t {username}/python-runtime-example:3.6.5
$ docker push {username}/python-runtime-example:3.6.5 
```

The `username` should be the one you have registered in Docker Hub. 

## Fetch Runtime

To add your newly created runtime to Serving just execute the following lines:

```sh
$ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "name": "{username}/python-runtime-example",
   "version": "3.6.5",
   "modelTypes": [
     "string"
   ],
   "tags": [
     "string"
   ],
   "configParams": {}
 }' 'http://localhost:8080/api/v1/runtime'
```

Here, `name` is your image published in the Docker Hub. `version` is the tag of that image. The last parameter (`http://localhost:8080/api/v1/runtime`) is where your Serving instance is running.

## Result

That's it. You've just created a simple runtime, that you can use in your own projects. It's almost an equal version of our [python runtime implementation](https://github.com/Hydrospheredata/hydro-serving-python). You can always look up details there. 

<br>
<hr>

# What's next? 

- [Learn, how you can contribute to Hydrosphere Serving]({{site.baseurl}}{%link dev.md%});
- [Learn, how you can deploy Serving on Kubernetes]({{site.baseurl}}{%link installation.md%}#kubernetes);
