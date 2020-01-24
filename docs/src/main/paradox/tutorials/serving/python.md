# Serving Python model

A Python model is a model that is backed with a Python runtime. You can create a Python model to perform any transformation, just provide an execution script packed with a serving function. In this tutorial we will create a simple increment function. 

@@toc { depth=1 }

## Before you start

We assume you already have a @ref[deployed](../../install/platform.md) instance of the Hydrosphere platform and a @ref[CLI](../../install/client.md#CLI) on your local machine.

To let `hs` know where the Hydrosphere platform runs, configure a new `cluster` entity. 

```sh 
$ hs cluster add --name local --server http://localhost
$ hs cluster use local
```

## Model structure

First of all, let's create a directory where we will put all of our code:

```sh
$ mkdir -p increment_model/src
$ cd increment_model
$ touch src/func_main.py
``` 

@@@ note
Generally, we use `hydrosphere/serving-runtime-python-3.6` runtime for serving Python models. This runtime uses `src/func_main.py` script as an entry point. You may create any arbitrary Python application within your model, just keep in mind that an entry point of your script has to be in `src/func_main.py`.
@@@

## Model dependencies

By default the `hydrosphere/serving-runtime-python-3.6` runtime does not have any scientific packages pre-installed, you would have to manage this yourself. Let's add `requirements.txt`:

```sh 
$ echo "tensorflow==1.14.0\nnumpy==1.17.2" > requirements.txt
```

## Serving function

Now let's implement the serving function, which will handle requests. Open `src/func_main.py` and paste the following code: 

Python
:   @@snip[serve.py](snippets/python/serve.py)

A `number` argument is a Protobuf object of type [hs.TensorProto](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor.proto). You cannot directly perform any computations on it (like add 2 alike protobuf messages together, etc), you would have to extract its value first. 

There are different ways of handling `hs.TensorProto` objects (one of which is described in the @ref[quickstart](../../getting-started/serving-simple-model.md#model-preparation)), but since we have a `tensorflow` package installed, we can use `tf.make_ndarray` method to transform a `number` protobuf to a numpy array.

After all computations, we need to pack the result back to `hs.TensorProto` object. The function returns a `hs.PredictResponse` object with a dictionary of tensors inside it.

@@@ note
If you use some external file (for example, such as @ref[model's weights](../../getting-started/serving-simple-model.md#model-preparation)), you would have to specify the absolute path to that file. By default, all files that you specify in the contract, are placed in the `/model/files` directory inside the runtime. 
@@@

## Model definition

To let Hydrosphere understand, what are the inputs and the outputs of your model you have to provide a definition of that model. Create a `serving.yaml` file inside your folder root.

@@@ vars
```yaml
kind: Model
name: "increment_model"
runtime: "hydrosphere/serving-runtime-python-3.6:$project.released_version$"
install-command: "pip install -r requirements.txt"   # this line will be executed during model build
payload:   # define all files of your model, that has to be packed
  - "src/"
  - "requirements.txt"

contract:  # time to remember your Python function signature
  name: increment  # name of signature is the name of the serving function
  inputs:
    number:
      shape: [-1]   # array of an arbitrary size
      type: int32
      profile: numerical
  outputs:
    number:
      shape: [-1]
      type: int32
      profile: numerical
```
@@@

@@@ note
If `.contract.name` is not set, it defaults to `predict`.
@@@

This file describes the model, its name, type, payload, and service contract. Contract declares input and output fields for the model; data types, shapes, and profile information. Profile information is used to calculate profiles of your training/production data.

That's it, you've just created a model that could be used within your business application. 

## Model deployment

Upload the model to the Hydrosphere platform.

```sh
$ hs upload
```

Once the model has been uploaded, it can be exposed for serving as an application. 

Create an application to declare an endpoint to your model. You can create it manually via Hydrosphere UI, or by providing an application manifest. 

```sh
$ hs apply -f - <<EOF
kind: Application
name: increment_app
singular:
  model: increment_model:1
EOF
```

## Prediction 

You can send requests with to your models using GRPC or HTTP endpoints, e.g.

```sh 
$ curl --request POST --header 'Content-Type: application/json' --header 'Accept: application/json' \
    --data '{ "number": [1] }' 'http://<host>/gateway/application/increment_app'
```
