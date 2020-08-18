# Develop runtimes

Sometimes our runtime images are not flexible enough. In that case, you might want to implement one yourself. 

The key things you need to know to write your own runtime are: 

* Knowledge of how to implement a predefined gRPC service for a dedicated language;
* Understanding of our contracts' protobufs to describe entry points, such as inputs and outputs;
* Knowledge of how to create your own docker image and publish it to an open registry.


## Generate GRPC code

There are different approaches to generating client and server gRPC code in [different languages](https://grpc.io/docs/). Let's have a look at how to do that in Python.

First, let's clone our [protos](https://github.com/Hydrospheredata/hydro-serving-protos) and prepare a folder for the generated code.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-protos
$ mkdir runtime
```

To generate the gRPC code we need additional packages. 

```sh
$ pip install grpcio-tools googleapis-common-protos
```

Our custom runtime will require `contracts` and `tf` protobuf messages. So let's generate them:

```sh
$ python -m grpc_tools.protoc --proto_path=./hydro-serving-protos/src/ --python_out=./runtime/ --grpc_python_out=./runtime/ $(find ./hydro-serving-protos/src/hydro_serving_grpc/contract/ -type f -name '*.proto')
$ python -m grpc_tools.protoc --proto_path=./hydro-serving-protos/src/ --python_out=./runtime/ --grpc_python_out=./runtime/ $(find ./hydro-serving-protos/src/hydro_serving_grpc/tf/ -type f -name '*.proto')
$ cd runtime
$ find ./hydro_serving_grpc -type d -exec touch {}/__init__.py \;
```

The structure of the `runtime` should now be as the follows:

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

Now that we have everything set up, let's actually implement a runtime. Create a `runtime.py` file and put in the following code:

Python
:   @@snip[runtime.py](snippets/python/develop-runtime/runtime.py)

Let's quickly review what we have here. `RuntimeManager` simply manages our service, i.e. starts it, stops it, and holds all necessary data. `RuntimeService` is a service that actually implements `Predict(PredictRequest)` RPC function.

The model will be stored inside the `/model` directory in the docker container. The structure of `/model` is a follows: 

```sh
model
├── contract.protobin
└── files
    ├── ...
    └── ...
```

`contract.protobin` file will be created by Manager service. It contains a binary representation of the [ModelContract](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_contract.proto) message. 

`files` directory contains all files of your model.

To run this service let's create an another file `main.py`.

Python
:   @@snip[main.py](snippets/python/develop-runtime/main.py)

## Publish Runtime

Before we can use the runtime, we have to package it into a container.

Add requirements for installing dependencies. Create a `requirements.txt` file with the following contents. 

```
grpcio==1.12.1 
googleapis-common-protos==1.5.3 
``` 

Create a Dockerfile to build our image. 

```docker
FROM python:3.6.5 

ADD . /app
RUN pip install -r /app/requirements.txt

ENV APP_PORT=9090

VOLUME /model 
WORKDIR /app

CMD ["python", "main.py"]
```

`APP_PORT` is an environment variable used by Hydrosphere. When Hydrosphere invokes `Predict` method, it does so via the defined port.

The structure of the `runtime` folder should now look like this:

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

Build and push the Docker image. 

```sh 
$ docker build -t {username}/python-runtime-example
$ docker push {username}/python-runtime-example

```

{% hint style="info" %}
Remember that the registry has to be accessible to the Hydrosphere platform so it can pull the runtime whenever it has to run a model with this runtime.
{% endhint %}

That's it. You have just created a simple runtime that you can use in your own projects. It  is an almost identical version of our [python runtime implementation](https://github.com/Hydrospheredata/hydro-serving-python). You can always look up details there. 
