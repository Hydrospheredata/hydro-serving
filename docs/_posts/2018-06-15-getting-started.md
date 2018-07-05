---
layout: post
title:  "Getting Started"
date:   2018-06-15
permalink: 'getting-started.html'
---

This document presents a quick way to set everything up and deploy your first model.


# Installation
{: #installation}

## ML Lambda

To get started, make sure, you have installed [Docker][docker-install] on your machine. Clone ML Lambda to the desired directory.

>Note: __hydro-serving__ is the working name of ML Lambda. 

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```

Now set up a docker environment. You have 2 options:

1. Lightweight version that doesn't contain any metrics and doesn't support Kafka. It will only allow you to deploy and run your models in a continuous manner. 

	```sh
	$ cd ./hydro-serving/
	$ docker-compose up --no-start
	```

2. Full version of ML Lambda with integrations to Kafka, Graphana, different metrics, etc. This might take a while. 
	```sh
	$ cd ./hydro-serving/integrations/
	$ docker-compose up --no-start
	```

>Note: If you've already installed one of the versions and want to install the other one, you may need to remove existing containers with `docker container rm $(docker container ls -aq)`.

After all images will be pulled, start ML Lambda.

```sh
$ docker-compose up
```

Open web-interface [http://127.0.0.1/][ml-lambda]. You are ready to go. 

## CLI

ML Lambda has a great [cli-tool][hydro-serving-cli], that lets you easily upload models. It supports Python 3.4 and above. To install, run:

```sh
$ pip install hs
```

# Uploading models
{: #uploading-models}

To get the notion of ML Lambda we recommend you to go through 2 bellow tutorials of uploading demo and own models. This will show you different aspects of working with model configuration, etc. 

## Uploading demo

### Fetching & uploading example models

We've already created a few [examples][hydro-serving-examples], that you can run to see, how everything works. Let's clone them and pick a model. 

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-example
$ cd ./hydro-serving-example/models/$MODEL_OF_YOUR_CHOICE
```

For the purpose of this tutorial we chose [stateful LSTM][stateful-lstm]. All you have to do is just to upload model to the server. 

> Note: At this stage you should have your ML Lambda instance running.

```sh
$ cd ./hydro-serving-example/models/stateful_lstm
$ hs upload --host "127.0.0.1" --port 8080
```

Stateful LSTM is actually a __Multi-Staged__ application, and includes additional parts: [pre][stateful-lstm-pre] and [post][stateful-lstm-post] processing stages. So we upload them as well.

```sh
$ cd ../stateful_lstm_preprocessing
$ hs upload --host "127.0.0.1" --port 8080
...
$ cd ../stateful_lstm_postprocessing
$ hs upload --host "127.0.0.1" --port 8080
```

Your models now have been uploaded to ML Lambda. You can find them here - [http://127.0.0.1/models][models] 

### Creating application

Let's create an application that can use our models. Open `Applications` page and press `ADD NEW`. Reproduce the following structure in the `Models` framework and create an application. 

| Stage | Model | Runtime | Description |
| ----- | ----- | ------- | ----------- |
| 1 | demo_preprocessing | hydrosphere/serving-runtime-python:3.6:latest | ... |
| 2 | stateful_lstm | hydrosphere/serving-runtime-tensorflow:1.7:latest | ... |
| 3 | demo_postprocessing | hydrosphere/serving-runtime-python:3.6:latest | ... |

Set _Application Name_ to `demo_lstm`. ML Lambda will automatically detect and fill models' signatures for you.

>Note: There's an option to add multiple models to one stage which might be confusing, because you may include all pipeline steps(pre, lstm, post) into a signle stage. Make sure, you've added new steps via `ADD NEW STAGE` button.

>Note, because we created a __Multi-staged__ application, ML Lambda automatically referred its contract. By default, the name of the signature in that contract is the name of the app. 

Invoking applications described in a section [below]({{site.baseurl}}{%link _posts/2018-06-15-getting-started.md%}#invoking-applications).

## Uploading own model
{: #upploading-own-model}

Because our examples were already configured for us, there isn't much work to do with them rather than just to upload. In this section we will start from scratch, create a simple linear regression model, configure it, upload to the server, and create an application to run it. 

### Creating model 

Create a directory for the model and add `model.py` inside it.

```sh
$ mkdir model
$ cd ./model 
$ touch model.py
```

Inside `model.py` we will define our model and train it.

`model.py`
```python
import os
import tensorflow as tf

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
```

There's actually 2 ways on running keras models inside ML Lambda: 
1. If Keras is used with TensorFlow backend, then it's possible to export `tf.session` and use it inside `hydrosphere/serving-runtime-tensorflow` runtime. 
2. Otherwize it's possible to use `hydrosphere/serving-runtime-python` runtime and define the work with the model in a python script. 

>Note: For this tutorial we will do the second approach, but if you want to get familiar with the first one, you can see this lnik. 

### Creating handler 

For running this model we will use Python Runtime. Internal structure of a model for this runtime should have `src` directory and contain `func_main.py` file inside it. 

```sh
$ mkdir src 
$ cd ./src
$ touch func_main.py
```

By default `manager` will put all of the files of our model inside `/model/files/` directory.

`func_main.py`
```python
import hydro_serving_grpc as hs
import numpy as np
from keras.models import load_model

model = load_model('/model/files/model.h5')

def infer(x):
    # 1. Prepare data points
    data = np.array(x.double_val)
    data = data.reshape(1, x.tensor_shape.dim[0].size)

    # 2. Make prediction
    result = model.predict(data)
    y = hs.TensorProto(
        double_val=result.reshape(len(result)).tolist(),
        tensor_shape=hs.TensorShapeProto(
            dim=[hs.TensorShapeProto.Dim(size=1)]),
        dtype=hs.DT_DOUBLE)

    # 3. Return the result
    return hs.PredictResponse(outputs={"y": y})
```

> Note: `Manager` interacts with a runtime via RPC. To write your own handlers you should understand our [Protobuf files][hydro-serving-protos]. 

### Defining model 

Because our model will be run inside a raw python container, it won't contain any required dependencies such as `keras` or `tensorflow`. Let's create a `requirements.txt` for that.

`requirements.txt`
```
keras==2.2.0
tensorflow==1.8.0
numpy==1.13.3
```

Right now ML Lambda does not understand what our model expects. Let's define `serving.yaml` and `contract.prototxt`. 

`serving.yaml` describes the model itself.
```yaml
model:
  name: "linear_regression"
  type: "python:3.6"
  contract: "contract.prototxt"
  payload:
    - "src/"
    - "requirements.txt"
    - "model.h5"
```

`contract.prototxt` describes the data, that will be feed to the model.

```
signatures {
    signature_name: "infer"
    inputs {
        name: "x"
        shape: {
    	    dim: {
    	        size: 2
    	    }
    	}
    	dtype: DT_DOUBLE
    }
    outputs {
        name: "y"
        shape: {
            dim: {
                size: 1
            }
        }
        dtype: DT_DOUBLE
    }
}
```

Overall structure of our model now should look like this:

```sh
model
├── contract.prototxt
├── model.h5
├── model.py
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

> Note: although, we have `model.py` inside directory, it won't be uploaded to ML Lambda since we didn't specify it in `serving.yaml`

### Uploading model

Now we're ready to upload our model.

```sh
$ hs upload --host "127.0.0.1" --port 8080
```

You can open [http://127.0.0.1/models][models] page to see uploaded model. 

>Note: You can use your models only when they're fully built and released. That is shown in their Status fields. 

### Creating Applicaion

Open `Applications` page and press `ADD NEW` button. As a runtime select `hydrosphere/serving-runtime-python` runtime and create application. Now you can [invoke]({{site.baseurl}}{%link _posts/2018-06-15-getting-started.md%}#running-applications) your app.

## Invoking Applications
{: #invoking-applications}

Invoking applications is available via different interfaces. 

1. __Test-request__

	You can test your application via web-interface. Press `Test` button in the application's page. Test data will be automatically pulled from model's contract. 

2. __HTTP-request__

	Send `POST` request to ML Lambda.

	```sh
	$ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
	   "data": [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1] 
	 }' 'http://localhost:8080/api/v1/applications/serve/1/demo_lstm'
	```

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
[ml-lambda]: http://127.0.0.1/
[models]: http://127.0.0.1/models
[applications]: http://127.0.0.1/applications
