# Serving Python Model

Python model is basically a function, which will be executed for every incoming request.
We use Python models to do custom computation (e.g. preprocessing) and run models which aren't supported yet.
You've got the idea. For the simplicity in this tutorial we will just implement a simple increment function.

## Writing Python code
First of all, let's create a directory where we will put all our code:
```sh
$ mkdir -p increment_model/src
$ cd increment_model
$ touch src/func_main.py
``` 

@@@ note
We generally use `hydrosphere/serving-runtime-python-3.6` docker image for serving Python model.
This image uses `src/func_main.py` script as entrypoint.
You may create any arbitrary Python application within your model, 
just keep in mind that an entry point to of your script has to be in `src/func_main.py`.
@@@

### Dependencies
By default the container where this model will be running does not have any scientific package pre-installed. 
You have to use `install-command` field to install dependencies from `requirements.txt` file.
Let's take tensorflow and numpy packages for this example:

```
tensorflow==1.12.0
numpy==1.14.3
```

Assume that `hydro_serving_grpc` is already installed.

### Implement an entrypoint

Now, let's write a simple code in `src/func_main.py`
```python
import tensorflow as tf
import hydro_serving_grpc as hs

def increment(number):   # <- keep in mind the signature
    request_number = tf.make_ndarray(number)
    response_number = requets_number + 1

    response_tensor_shape = [hs.TensorShapeProto.Dim(size=dim) for dim in number.tensor_shape.dim]
    response_tensor = hs.TensorProto(
        int_val=response_number.flatten(), 
        dtype=hs.DT_INT32,
        tensor_shape=hs.TensorShapeProto(dim=response_tensor_shape)
    )

    return hs.PredictResponse(outputs={"number": response_tensor})
```
`number` function argument is a Protobuf object of type 
[hs.TensorProto](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor.proto)
Before using it, we need to extract it's value. There are different ways of handling `hs.TensorProto` objects,
and since we have tensorflow package, we will use `tf.make_ndarray` to transform `number` to ndarray.

After all computations, we need to pack the result back to `hs.TensorProto` object.
The function returns a `hs.PredictResponse` with dictionary of tensors inside it.

@@@ note
If you use some external file (@ref[such as model's weights](index.md#preparing-the-model)), 
you would have to specify the absolute path to that file.
That's due to some limitations of `hydrosphere/serving-runtime-python-3.6` image.
@@@

### Write a Model definition

For serving Python models we use `hydrosphere/serving-runtime-python-3.6` docker image.
This image requires `src/func_main.py` script, where it will search for a proper function. 

Create a `serving.yaml` file inside your folder root.

```yaml
kind: Model
name: "increment_model"
runtime: "hydrosphere/serving-runtime-python-3.6:dev"
install-command: "pip install -r requirements.txt"   # this line will be executed during model build
payload:   # define your model files
  - "src/"
  - "requirements.txt"

contract:  # time to remember your Python function signature
  name: increment  # name of signature == python function name
  inputs:
    number:
      shape: [-1]   # array of an arbitrary size
      type: int32
      profile: numerical   # determines the statistics for your field
  outputs:
    number:
      shape: [-1]
      type: int32
      profile: numerical
```

@@@ note
If `contract.name` is not set, it defaults to `predict` value.
@@@

This file describes the model, its name, type, payload files and contract. 
Contract declares input and output fields for the model;
data types, shapes and profile information. 

That's it, you've just created a model that could be used within your business application. 

@@@ note
You can check model status using web-UI or `hs model list` command.
@@@

### Serve the model 

Upload the model to serving cluster.

```sh
$ hs upload
```

Now the model is uploaded to the serving service but does not yet available for the invocation.

Create an application to declare an endpoint to your model. 
You can create it manually via UI interface, or by providing an application manifest. 
You can do it using Applications page in web-UI and create a new app which will use `mnist` model. 

Alternatively, you can create an application using hs CLI:

```sh
hs apply -f - <<EOF
kind: Application
name: increment_app
singular:
  model: increment_model:1
EOF
```

You can send requests with GRPC or HTTP endpoints, e.g.

```sh 
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ "number": [1] }' 'https://<host>/gateway/applications/increment_app'
```