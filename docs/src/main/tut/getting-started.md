---
layout: docs
title:  "Getting Started"
permalink: 'getting-started.html'
---

# Getting Started
This document presents a quick way to set everything up and deploy your first model.

## Installation
To get started, make sure you have installed [Docker][docker-install] on your machine. Clone ML Lambda to the desired directory.

```sh
$ cd ~/
$ git clone https://github.com/Hydrospheredata/hydro-serving
```
Now set up a docker environment. You have 2 options:

1. The __lightweight__ version lets you manage your models in a continuous manner, version them, create applications that use your models and deploy all of this into production. To do that, just execute the following: 

    ```sh
    $ cd ~/hydro-serving/
    $ docker-compose up
    ```

2. The __integrations__ version extends the lightweight version and lets you also integrate kafka, graphana and influxdb .

    ```sh
    $ cd ~/hydro-serving/integrations/
    $ docker-compose up
    ```

_Note: If you've already installed one of the versions and want to install the other one, you'll need to remove coinciding containers defined in `docker-compose.yaml` (those are placed in both __lightweight__ and __integrations__ versions)._ 

Open web interface [http://localhost/][ml-lambda].  You are ready to go. 

### CLI

ML Lambda has CLI tool that is used to upload your models to the server. It supports Python 3.4 and above. To install, run:

```sh
$ pip install hs
```

To work with ML Lambda you'd need to set up a cluster, that will be used to upload your models to. 

```sh
$ hs cluster add --name local --server http://localhost
```

To learn more about clusters check the [CLI]({{site.baseurl}}{%link cli.md%}) page. 

## Uploading models

To get the notion of ML Lambda we recommend you to go through 2 bellow tutorials of uploading [demo model]({{site.baseurl}}{%link getting-started.md%}#demo) and [own model]({{site.baseurl}}{%link getting-started.md%}#owm-model). This will show you different aspects of working with model configuration, building multi-staged, single-staged applications, etc. 

<hr>

### Demo model

We've already created a few [examples][hydro-serving-examples] that you can run to see, how everything works. Let's clone them and pick a model. 

```sh
$ cd ~/
$ git clone https://github.com/Hydrospheredata/hydro-serving-example
$ cd ~/hydro-serving-example/models/$MODEL_OF_YOUR_CHOICE
```

#### Fetching & uploading Stateful LSTM

For the purpose of this tutorial we chose [Stateful LSTM][stateful-lstm]. All you have to do is just to upload the model to the server. 

```sh
$ cd ~/hydro-serving-example/models/stateful_lstm
$ hs upload 
```

Stateful LSTM is actually a __Multi-Staged__ application. It includes additional parts: [pre][stateful-lstm-pre] and [post][stateful-lstm-post] processing stages. So we upload them as well.

```sh
$ cd ~/hydro-serving-example/models/stateful_lstm_preprocessing
$ hs upload
...
$ cd ~/hydro-serving-example/models/stateful_lstm_postprocessing
$ hs upload 
```

Your models now have been uploaded to ML Lambda. You can find them here - [http://localhost/models][models] 

#### Creating an application

Let's create an application that can use our models. Open _Applications_ page and press _Add New_ and reproduce the following structure in the _Models_ section. 

| Stage | Model | Runtime |
| ----- | ----- | ------- |
| Stage_1 | demo_preprocessing | hydrosphere/serving-runtime-python:3.6:latest |
| Stage_2 | stateful_lstm | hydrosphere/serving-runtime-tensorflow:1.7.0:latest |
| Stage_3 | demo_postprocessing | hydrosphere/serving-runtime-python:3.6:latest |

<br>
_Note: There's an option to add multiple models to one stage which might be confusing, because you may include all pipeline steps(pre, lstm, post) into a signle stage. Make sure, you've added new steps via `Add New Stage` button._

When creating an application, ML Lambda will automatically infer contract for it as well as for models. If it's a __single-staged__ application, it will look up model's signatures and take one from there, if it's a __multi-staged__ application, it will create a signature with `signature_name` equal to the application's name.

Invoking applications described in a section [below]({{site.baseurl}}{%link getting-started.md%}#invoking-applications).

<hr>

### Own model

In this section we will start from scratch, create a simple linear regression model, configure it, upload to the server and create an application for it. 

#### Creating the model 

Create a directory for the model and add `model.py` inside it.

```sh
$ mkdir ~/linear_regression
$ cd ~/linear_regression
$ touch model.py
```

Inside `model.py` we will define our model, train it and save. 

```python
from keras.models import Sequential
from keras.layers import Dense
from sklearn.datasets import make_regression
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler

# initialize data
n_samples = 1000
X, y = make_regression(n_samples=n_samples, n_features=2, noise=0.5, random_state=112)

scallar_x, scallar_y = MinMaxScaler(), MinMaxScaler()
scallar_x.fit(X)
scallar_y.fit(y.reshape(n_samples, 1))
X = scallar_x.transform(X)
y = scallar_y.transform(y.reshape(n_samples, 1))

# create a model
model = Sequential()
model.add(Dense(4, input_dim=2, activation='relu'))
model.add(Dense(4, activation='relu'))
model.add(Dense(1, activation='linear'))

model.compile(loss='mse', optimizer='adam')
model.fit(X, y, epochs=1000, verbose=0)

# save model
model.save('model.h5')
```

There's actually 2 ways to run keras models inside ML Lambda: 
1. If Keras is used with TensorFlow backend, then it's possible to export `tf.session` and use it inside `hydrosphere/serving-runtime-tensorflow` runtime. 
2. Otherwise it's possible to use `hydrosphere/serving-runtime-python` runtime and define all actions in a python script. 

_Note: For this tutorial we will do the second approach. If you want to get familiar with the first one, you can visit [Models]({{site.baseurl}}{%link models.md%}#uploading-keras) page._

#### Creating a handler 

For running this model we will use the Python Runtime. Internal structure of a model for this runtime should have `src` directory and contain `func_main.py` file inside it. 

```sh
$ mkdir src 
$ cd src
$ touch func_main.py
```

By default ML Lambda will put all of the files of our model inside `/model/files/` directory. So, we load our model from there. 

```python
import hydro_serving_grpc as hs
import numpy as np
from keras.models import load_model

model = load_model('/model/files/model.h5')

def infer(x):
    # 1. Prepare data points
    data = np.array(x.double_val)
    data = data.reshape([dim.size for dim in x.tensor_shape.dim])

    # 2. Make prediction
    result = model.predict(data)
    y = hs.TensorProto(
        dtype=hs.DT_DOUBLE,
        double_val=result.flatten(),
        tensor_shape=hs.TensorShapeProto(dim=[hs.TensorShapeProto.Dim(size=-1)]))

    # 3. Return the result
    return hs.PredictResponse(outputs={"y": y})
```

_Note: ML Lambda interacts with a runtime container via gRPC interface._ 

#### Describing the model 

Since our model will be running inside a raw Python container, the container won't have any required dependencies pre-installed. Let's create a `requirements.txt` for that.

```
keras==2.2.0
tensorflow==1.8.0
numpy==1.13.3
```

Right now ML Lambda does not understand what our model expects. Let's define `serving.yaml` contract. 

```yaml
kind: Model
name: linear_regression
model-type: python:3.6
payload:
  - "src/"
  - "requirements.txt"
  - "model.h5"

contract:
  infer:
    inputs:
      x:
        shape: [-1, 2]
        type: double
        profile: numerical
    outputs:
      y:
        shape: [-1]
        type: double
        profile: numerical
```

Here we've decsribed the type of the model, it's files, some additional artefacts and defined the inputs and outputs of the model. For more information check [Models]({{site.baseurl}}{%link models.md%}#servingyaml) page. 

Overall structure of our model now should look like this:

```sh
linear_regression
├── model.h5
├── model.py
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

_Note: although, we have `model.py` inside directory, it won't be uploaded to ML Lambda since we didn't specify it in payload._

#### Uploading the model

Now we're ready to upload our model.

```sh
$ hs upload
```

You can open [http://localhost/models][models] page to see uploaded model. 

#### Creating an applicaion

Open `Applications` page and press `Add New` button. Select your model and as a runtime select `hydrosphere/serving-runtime-python` then create an application. Now you can [invoke]({{site.baseurl}}{%link getting-started.md%}#invoking-applications) your app. 

_Note: If you cannot find your newly uploaded model and it's listed in your models page, that means, it's still in a building stage. Wait until the model changes its status to `Released`, then you can use it._

<hr>

## Invoking applications

Invoking applications is available via different interfaces. 

### Test request

You can perform test request to the model from interface. Open desired application and press `Test` button. Internally it will generate arbitrary input data from model's contract and send an HTTP-request to API endpoint. 

### HTTP-request

Send `POST` request to ML Lambda. 

1. [demo_lstm]({{site.baseurl}}{%link getting-started.md%}#uploading-demo)

    ```sh
    $ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
    "data": [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1] 
    }' 'http://localhost/gateway/applications/demo_lstm/demo_lstm'
    ```

2. [linear_regression]({{site.baseurl}}{%link getting-started.md%}#own-model)

    ```sh
    $ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
    "x": [[1, 1],[1, 1]]}' 'http://localhost/gateway/applications/linear_regression/infer'
    ```

### gRPC API call

You can define a gRPC client on your side and make a call from it. Here we provide a Python example, but this can be done in any language. 

```python
import grpc 
import hydro_serving_grpc as hs

# connect to your ML Lamba instance
channel = grpc.insecure_channel("localhost")
stub = hs.PredictionServiceStub(channel)

# 1. define a model, that you'll use
model_spec = hs.ModelSpec(name="linear_regression", signature_name="infer")
# 2. define tensor_shape for Tensor instance
tensor_shape = hs.TensorShapeProto(dim=[hs.TensorShapeProto.Dim(size=-1), hs.TensorShapeProto.Dim(size=2)])
# 3. define tensor with needed data
tensor = hs.TensorProto(dtype=hs.DT_DOUBLE, tensor_shape=tensor_shape, double_val=[1,1,1,1])
# 4. create PredictRequest instance
request = hs.PredictRequest(model_spec=model_spec, inputs={"x": tensor})

# call Predict method
result = stub.Predict(request)
```

_Note: For convinience we've already generated all our proto files to a python library and published it in PyPI. You can install it via `pip install hydro-serving-grpc`_

[docker-install]: https://docs.docker.com/install/
[docker-hub]: https://hub.docker.com/u/hydrosphere/
[docker-hub-python]: https://hub.docker.com/u/hydrosphere/serving-runtime-python/
[docker-hub-spark]: https://hub.docker.com/r/hydrosphere/serving-runtime-spark/
[docker-hub-tensorflow]: https://hub.docker.com/u/hydrosphere/serving-runtime-tensorflow/

[hydro-sonar]: https://hydrosphere.io/sonar/
[hydro-serving-cli]: https://github.com/Hydrospheredata/hydro-serving-cli
[hydro-serving-examples]: https://github.com/Hydrospheredata/hydro-serving-example
[hydro-serving-protos]: https://github.com/Hydrospheredata/hydro-serving-protos
[appache-kafka]: https://kafka.apache.org
[stateful-lstm]: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models/stateful_lstm
[stateful-lstm-pre]: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models/stateful_lstm_preprocessing
[stateful-lstm-post]: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models/stateful_lstm_postprocessing
[ml-lambda]: http://localhost/
[models]: http://127.0.0.1/models
[applications]: http://127.0.0.1/applications
