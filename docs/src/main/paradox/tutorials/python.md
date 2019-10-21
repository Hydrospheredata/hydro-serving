# Serving Python model

Python model is a model that is backed with a Python runtime. You can 
create a Python model to perform any transformation, just provide an 
execution script packed with a serving function. In this tutorial we 
will create a simple increment function. 


## Model Structure

First of all, let's create a directory where we will put all our code:

```sh
mkdir -p increment_model/src
cd increment_model
touch src/func_main.py
``` 

@@@ note
Generally we use `hydrosphere/serving-runtime-python-3.6` runtime for 
serving Python models. This runtime uses `src/func_main.py` script as 
an entry point. You may create any arbitrary Python application within 
your model, just keep in mind that an entry point to of your script has 
to be in `src/func_main.py`.
@@@

## Dependencies

By default the `hydrosphere/serving-runtime-python-3.6` runtime does 
not have any scientific packages pre-installed, you have to manage this 
yourself. Let's add `requirements.txt`: 

```sh 
echo "tensorflow==1.12.0\nnumpy==1.14.3" > requirements.txt
```

## Function

Now let's implement the serving function, which will handle requests. 
Open `src/func_main.py` and paste the following code: 

```python
import tensorflow as tf
import hydro_serving_grpc as hs  # this package is already present in the runtime

def increment(number):   # <- keep in mind the signature
    request_number = tf.make_ndarray(number)
    response_number = requets_number + 1

    response_tensor_shape = [
        hs.TensorShapeProto.Dim(size=dim) for dim in number.tensor_shape.dim]
    response_tensor = hs.TensorProto(
        int_val=response_number.flatten(), 
        dtype=hs.DT_INT32,
        tensor_shape=hs.TensorShapeProto(dim=response_tensor_shape)
    )

    return hs.PredictResponse(
        outputs={"number": response_tensor})
```

A `number` argument is a Protobuf object of type [hs.TensorProto](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor.proto).
You cannot directly perform any computations (like add to 2 protobuf 
messages together, etc), you would have to extract its values first. 

There are different ways of handling `hs.TensorProto` objects (one of 
which is described in the @ref[quickstart](quickstart.md#model-preparation)), 
but since we have `tensorflow` package installed, we can use `tf.make_ndarray` 
method to transform `number` to `numpy.ndarray`.

After all computations, we need to pack the result back to `hs.TensorProto` 
object. The function returns a `hs.PredictResponse` with dictionary of tensors 
inside it.

@@@ note
If you use some external file (for example, such as 
@ref[model's weights](quickstart.md#model-preparation)), you would have to 
specify the absolute path to that file. By default, all files, that you specify 
in the contract, are placed in the `/model/files` directory inside the runtime. 
@@@

## Model Definition

To let cluster understand, what are the inputs and the outputs of your model 
you have to provide a definition of that model. Create a `serving.yaml` file 
inside your folder root.

```yaml
kind: Model
name: "increment_model"
runtime: "hydrosphere/serving-runtime-python-3.6:0.1.2-rc0"
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

@@@ note
If `contract.name` is not set, it defaults to `predict`.
@@@

This file describes the model, its name, type, payload and service contract. 
Contract declares input and output fields for the model; data types, shapes 
and profile information. Profile information is used to calculate profiles 
of your training/production data.

That's it, you've just created a model that could be used within your 
business application. 

## Deployment

Upload the model to a serving cluster.

```sh
hs upload
```

Once the model has been uploaded, it can be exposed for serving as an 
application. 

Create an application to declare an endpoint to your model. You can create 
it manually via UI interface, or by providing an application manifest. 

```sh
hs apply -f - <<EOF
kind: Application
name: increment_app
singular:
  model: increment_model:1
EOF
```

## Inference 

You can send requests with to your models using GRPC or HTTP endpoints, e.g.

```sh 
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ "number": [1] }' 'https://<host>/gateway/applications/increment_app'
```
