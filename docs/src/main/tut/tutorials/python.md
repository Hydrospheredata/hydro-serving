---
layout: docs
title:  "Serving Python Model"
permalink: "python.html"
---

# Serving Python Model

When we talk about Python models we mean any arbitrary actions that can be done with Python. You can add some number to your inputs, process images, serve models saved in binary format (scikit-learn, Keras, gluon-cv), etc. You've got the idea. For the simplicity in this tutorial we will do just that - increment an input number by one.

### Create a handler

Python models have to follow a specific directory structure in order to be used with `hydrosphere/serving-runtime-python` runtime. Create a directory `increment_model`. 

```sh
$ mkdir -p increment_model/src
$ cd increment_model
$ touch src/func_main.py
``` 

The model directory must contain a `src` folder with `func_main.py` file inside. This would be the main file used by the runtime. You may create any arbitrary Python application within your model, just keep in mind that an entry point have to be `src/func_main.py`.

```python
# src/func_main.py

import tensorflow as tf
import hydro_serving_grpc as hs

def increment(number):
    request_number = tf.make_ndarray(number)
    response_number = requets_number + 1

    response_tensor_shape = [hs.TensorShapeProto.Dim(size=dim) for dim in number.tensor_shape.dim]
    response_tensor = hs.TensorProto(
        int_val=response_number.flatten(), dtype=hs.DT_INT32,
        tensor_shape=hs.TensorShapeProto(dim=response_tensor_shape))

    return hs.PredictResponse(outputs={"number": response_tensor})
```

There are different ways of handling incoming tensors. Here we've used a Tensorflow function `tf.make_ndarray` to create a numpy ndarray from a raw [TensorProto](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor.proto)-like structure `number`. Then we've added a one to that ndarray, packed it back to a response tensor and returned it as a result. 

### Define dependencies 

By default the container where this model will be running does not have any scientific package pre-installed. You have to provide a `requirements.txt` file with the dependencies to let ML Lambda know which Python packages to install during model building. We've used a Tensorflow and Numpy packages in this example:

```
tensorflow==1.12.0
numpy==1.14.3
```

### Write a manifest 

Since we have defined a function-handler, we have to tell ML Lambda what to call from `src/func_main.py`. In this case it's the `increment` function. Create a `serving.yaml` manifest. 

```yaml
# serving.yaml

kind: Model
name: "increment_model"
model-type: "python:3.6"
payload: 
  - "src/"
  - "requirements.txt"

contract:
  increment:                  # Signature function
    inputs:
      number:                 # Input field name
        shape: [-1]
        type: int32
        profile: numeric
    outputs:
      number:                 # Output field name
        shape: [-1]
        type: int32
        profile: numeric
```

This file describes the model, its name, type, payload files and contract. Contract declares input and output fields for the model; data types, shapes and profile information. 

That's it, you've just created a simple model which you can use within your business applications. 

### Serve the model 

Upload the model to ML Lambda.

```sh
$ hs upload
```

Now the model is uploaded to the serving service but does not yet available for the invokation. Create an application to declare an endpoint to your model. You can create it manually via UI interface, or by providing an application manifest. To do it with web interface, open your `http://<host>/` where ML Lambda has been deployed, open Applications page and create a new app which will use `mnist` model. Or by manifest:

```yaml
# application.yaml 

kind: Application
name: increment_app
singular:
  model: increment_model:1
  runtime: hydrosphere/serving-runtime-python:3.6-latest
```

```sh
$ hs apply -f application.yaml
```

That's it, now you can increment numbers. 

```sh 
$ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ "number": [1] }' 'https://<host>/gateway/applications/increment_app/increment'
```

<br>
<hr>

# What's next?

- [Learn, how to work with CLI]({{site.baseurl}}{%link concepts/cli.md%});
- [Learn, how to serve Tensorflow models]({{site.baseurl}}{%link tutorials/tensorflow.md%});
- [Learn, how to invoke applications]({{site.baseurl}}{%link concepts/applications.md%});
- [Learn, how to write manifests]({{site.baseurl}}{%link how-to/write-manifests.md%});
