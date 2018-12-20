---
layout: docs
title:  "Upload Python Model"
permalink: "python.html"
---

# Upload Python Model

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

def infer(number):
    request_number = tf.make_ndarray(number)
    response_number = requets_number + 1

    response_tensor_shape = [hs.TensorShapeProto.Dim(size=dim) for dim in number.tensor_shape.dim]
    response_tensor = hs.TensorProto(
        int_val=response_number.flatten(), dtype=hs.DT_INT32,
        tensor_shape=hs.TensorShapeProto(dim=response_tensor_shape))

    return hs.PredictResponse(outputs={"number": response_tensor})
```

There are different ways of handling incoming tensors. Here we've used a Tensorflow function `tf.make_ndarray` to create a numpy ndarray from a raw TensorProto-like structure `number`. Then we've added a one to that ndarray, packed it to a response tensor and returned it as a result. 

### Define dependencies 

By default the container where this model will be running does not have any scientific package pre-installed. You have to provide a `requirements.txt` file with the dependencies to let ML Lambda know which Python packages to install during model building. We've used a Tensorflow and Numpy packages in this example:

```
tensorflow==1.12.0
numpy==1.14.3
```

### Write contract

Now that we have defined handler, we have to tell ML Lambda which function to call from `src/func_main.py`. In our case it's the `infer` function. Create a `serving.yaml` manifest. 

```yaml
# serving.yaml

kind: Model
name: "increment_model"
model-type: "python:3.6"
payload: 
  - "src/"
  - "requirements.txt"

contract:
  infer:                      # Signature function
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

That's it, you've just created a simple model which you can use within your business applications. To upload the model use:

```sh
$ hs upload
```

# What's next?

- Learn, how to work with CLI;
- Learn, how to upload Tensorflow models;
- Learn, how to invoke models;
- Learn, how to write Contracts;
