---
description: >-
  This is an entry-point tutorial to the Hydrosphere platform. Estimated
  completion time: 13 min.
---

# Getting Started

## Overview

In this tutorial, you will learn the basics of working with Hydrosphere. We will prepare an example model for serving, deploy it to Hydrosphere, turn it into an application, invoke it locally, and use monitoring. As an example model, we will take a simple logistic regression model fit with randomly generated data, with some noise added to it.

By the end of this tutorial you will know how to:

* Prepare a model for Hydrosphere
* Serve a model on Hydrosphere
* Create an Application
* Invoke an Application
* Use basic monitoring

## Prerequisites

For this tutorial, you need to have **Hydrosphere Platform** deployed and **Hydrosphere CLI** \(`hs`\) along with **Python SDK** \(`hydrosdk`\) \_\*\*\_installed on your local machine. If you don't have them yet, please follow these guides first:

* [Platform Installation](installation/)
* [CLI](https://hydrosphere.gitbook.io/home/installation/cli)
* [SDK](installation/sdk.md)

To let `hs` know where the Hydrosphere platform runs, configure a new `cluster` entity:

```bash
hs cluster add --name local --server http://localhost
hs cluster use local
```

## Before you start

In the next two sections, we will prepare a model for deployment to Hydrosphere. It is important to stick to a specific folder structure during this process to let `hs` parse and upload the model correctly. Make sure that the structure of your local model directory looks like this by the end of the model preparation section:

```text
logistic_regression
├── model.joblib
├── train.py
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

* **`train.py`** - a training script for our model
* **`requirements.txt`** - provides dependencies for our model
* **`model.joblib`** - a model artifact that we get as a result of model training
* **`src/func_main.py`** - an inference script that defines a function for making model predictions
* **`serving.yaml`** - a resource definition file to let Hydrosphere know which function to call from the `func_main.py` script and let the model manager understand model’s inputs and outputs.

## Training a model

While Hydrosphere is a post-training platform, let's start with basic training steps to have a shared context.

As mentioned before, we will use the logistic regression model `sklearn.LogisticRegression`. For data generation, we will use the `sklearn.datasets.make_regression` \([link](https://scikit-learn.org/stable/modules/generated/sklearn.datasets.make_regression.html)\) method.

First, create a directory for your model and create a new `train.py` inside:

```bash
mkdir logistic_regression
cd logistic_regression
touch train.py
```

Put the following code for your model in the `train.py` file:

{% code title="train.py" %}
```python
import joblib
from sklearn.datasets import make_blobs
from sklearn.linear_model import LogisticRegression

# initialize data
X, y = make_blobs(n_samples=300, n_features=2, centers=[[-5, 1],[5, -1]])

# create a model
model = LogisticRegression()
model.fit(X, y)

joblib.dump(model, "model.joblib")
```
{% endcode %}

Next, we need to install all the necessary libraries for our model. In your `logistic_regression` folder, create a `requirements.txt` file and provide dependencies inside:

{% code title="requirements.txt" %}
```text
numpy~=1.18
scipy==1.4.1
scikit-learn~=0.23
joblib~=0.15
```
{% endcode %}

Install all the dependencies to your local environment:

```bash
$ pip install -r requirements.txt
```

Train the model:

```bash
$ python train.py
```

As soon as the script finishes, you will get the model saved to a `model.joblib` file.

## Model preparation

Every model in the Hydrosphere cluster is deployed as an individual container. After a request is sent from the client application, it is passed to the appropriate Docker container with your model deployed on it. An important detail is that all model files are stored in the `/model/files` directory inside the container, so we will look there to load the model.

To run our model we will use a Python runtime that can execute any Python code you provide. Model preparation is pretty straightforward, but you have to create a specific folder structure described in the "Before you start" section.

### Provide the inference script

Let's create the main file `func_main.py`in the `/src` folder of your model directory:

```bash
mkdir src
cd src
touch func_main.py
```

Hydrosphere communicates with the model using [TensorProto](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor.proto) messages. If you want to perform a transformation or inference on the received TensorProto message, you will have to retrieve its contents, perform a transformation on it, and pack the result back to the TensorProto message. Pre-built python runtime automatically converts TensorProto messages to Numpy arrays, so the end-user doesn't need to interact with TensorProto messages.

To do inference you have to define a function that will be invoked every time Hydrosphere handles a request and passes it to the model. Inside that function, you have to call a `predict` \(or similar\) method of your model and return your predictions:

{% code title="func\_main.py" %}
```python
import joblib
import numpy as np

# Load a model once
model = joblib.load("/model/files/model.joblib")

def infer(x1, x2):

    # Make a prediction
    y = model.predict([[x1, x2]])

    # Return the scalar representation of y
    return {"y": y.item()}
```
{% endcode %}

Inside `func_main.py` we initialize our model outside of the serving function `infer.`This process will not be triggered every time a new request comes in.

The `infer` function takes the actual request, unpacks it, makes a prediction, packs the answer, and returns it. There is no strict rule for naming this function, it just has to be a valid Python function name.

### Provide a resource definition file

To let Hydrosphere know which function to call from the `func_main.py` file, we have to provide a resource definition file. This file will define a function to be called, inputs and outputs of a model, a signature function, and some other metadata required for serving.

Create a resource definition file `serving.yaml`in the root of your model directory`logistic_regression`:

```bash
cd ..
touch serving.yaml
```

Inside `serving.yaml` we also provide`requirements.txt` and`model.joblib` as payload files to our model:

{% code title="serving.yaml" %}
```yaml
kind: Model
name: logistic_regression
runtime: hydrosphere/serving-runtime-python-3.7:2.3.2
install-command: pip install -r requirements.txt
payload:
  - src/
  - requirements.txt
  - model.joblib

contract:
  name: infer
  inputs:
    x1:
      shape: scalar
      type: double
      profile: numerical
    x2:
      shape: scalar
      type: double
      profile: numerical
  outputs:
    y:
      shape: scalar
      type: int64
      profile: categorical
```
{% endcode %}

At this point make sure that the overall structure of your local model directory looks as shown in the "Before you start" section.

{% hint style="info" %}
Although we have `train.py` inside the model directory, it will not be uploaded to the cluster since we are not listing it under`payload` in the resource definition file.
{% endhint %}

## Serving a Model

Now we are ready to upload our model to Hydrosphere. To do so, inside the `logistic_regression` model directory run:

```bash
hs upload
```

To see your uploaded model, open [http://localhost/models](http://localhost/models).

If you cannot find your newly uploaded model and it is listed on your models' page, it is probably still in the building stage. Wait until the model changes its status to `Released`, then you can use it.

## Creating an Application

Once you have opened your model in the UI, you can create an **application** for it. Basically, an application represents an endpoint to your model, so you can invoke it from anywhere. To learn more about advanced features, go to the [Applications](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/getting-started/concepts/applications.md) page.

![Creating an Application from the uploaded model](../.gitbook/assets/application_creation%20%281%29%20%282%29.gif)

Open [http://localhost/applications](http://localhost/applications) and press the `Add New Application` button. In the opened window select the `logistic_regression` model, name your application `logistic_regression` and click the "Add Application" button.

## Invoking an application

Invoking applications is available via different interfaces. For this tutorial, we will cover calling the created Application by gRPC via our Python SDK.

To install SDK run:

```text
pip install hydrosdk
```

Define a gRPC client on your side and make a call from it:

{% code title="send\_data.py" %}
```python
from sklearn.datasets import make_blobs
from hydrosdk import Cluster, Application

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "logistic_regression")
predictor = app.predictor()

X, _ = make_blobs(n_samples=300, n_features=2, centers=[[-5, 1],[5, -1]])
for sample in X:
    y = predictor.predict({"x1": sample[0], "x2": sample[1]})
    print(y)
```
{% endcode %}

## Getting Started with Monitoring

Hydrosphere Platform has multiple tools for data drift monitoring:

1. Data Drift Report
2. Automatic Outlier Detection
3. Profiling

In this tutorial, we'll look at the **monitoring dashboard** and **Automatic Outlier Detection** feature.

{% hint style="info" %}
Hydrosphere Monitoring relies heavily on training data. Users **must** provide training data to enable monitoring features.
{% endhint %}

### Provide training data

To provide training data users need to add the `training-data=<path_to_csv>` field to the `serving.yaml` file. Run the following script to save training data used in previous steps as a `trainig_data.csv` file:

{% code title="save\_training\_data.py" %}
```python
import pandas as pd
from sklearn.datasets import make_blobs

# Create training data
X, y = make_blobs(n_samples=300, n_features=2, centers=[[-5, 1],[5, -1]])

# Create pandas.DataFrame from it
df = pd.DataFrame(X, columns=['x1', 'x2'])
df['y'] = y

# Save it as .csv
df.to_csv("training_data.csv", index=False)
```
{% endcode %}

Next, add the training data field to the model definition inside the `serving.yaml` file:

{% code title="serving.yaml" %}
```yaml
kind: Model
name: logistic_regression
runtime: hydrosphere/serving-runtime-python-3.7:2.3.2
install-command: pip install -r requirements.txt
training-data: training_data.csv
payload:
  - src/
  - requirements.txt
  - model.joblib
contract:
  name: infer
  inputs:
    x1:
      shape: scalar
      type: double
      profile: numerical
    x2:
      shape: scalar
      type: double
      profile: numerical
  outputs:
    y:
      shape: scalar
      type: int64
      profile: categorical
```
{% endcode %}

### Upload a model

Now we are ready to upload our model. Run the following command to create a new version of the `logistic_regresion` model:

```bash
hs upload
```

Open the [http://localhost/models](http://localhost/models) page to see that there are now two versions of the`ogistic_regression` model.

For each model with uploaded training data, Hydrosphere creates an outlier detection metric, which assigns an outlier score to each request. This metric labels a request as an outlier if the outlier score is greater than the 97th percentile of training data outlier scores distribution.

### Update an Application

Let's send some data to our new model version. To do so, we need to update our `logistic_regression` application. To update it, we can go to the **Application** tab and click the "Update" button:

![Upgrading an application stage to a newer version](../.gitbook/assets/application_upgrade%20%281%29%20%282%29.gif)

### Send data to Application

After updating our Application, we can reuse our old code to send some data:

{% code title="send\_data.py" %}
```python
from sklearn.datasets import make_blobs
from hydrosdk import Cluster, Application

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "logistic_regression")
predictor = app.predictor()

X, _ = make_blobs(n_samples=300, n_features=2, centers=[[-5, 1],[5, -1]])
for sample in X:
    y = predictor.predict({"x1": sample[0], "x2": sample[1]})
    print(y)
```
{% endcode %}

### Monitor data quality

You can monitor your data quality in the Monitoring Dashboard:

![](../.gitbook/assets/image%20%283%29%20%282%29.png)

The Monitoring dashboard plots all requests streaming through a model version as rectangles colored according to how "healthy" they are. On the horizontal axis, we group our data by batches and on the vertical axis, we group data by signature fields. In this plot, cells are determined by their batch and field. Cells are colored from green to red, depending on the average request health inside this batch.

### Check data drift detection

To check whether our metric will be able to detect data drifts, let's simulate one and send data from another distribution. To do so, let's slightly modify our code:

{% code title="send\_bad\_data.py" %}
```python
from sklearn.datasets import make_blobs
from hydrosdk import Cluster, Application

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "logistic_regression")
predictor = app.predictor()

# Change make_blobs arguments to simulate different distribution 
X, _ = make_blobs(n_samples=300, n_features=2, centers=[[-10, 10],[0, 0]])
for sample in X:
    y = predictor.predict({"x1": sample[0], "x2": sample[1]})
    print(y)
```
{% endcode %}

You can validate that your model was able to detect data drifts on the monitoring dashboard.

