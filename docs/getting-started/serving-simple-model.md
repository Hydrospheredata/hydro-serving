# Serving a simple model

On this page you will learn how to deploy your first model on the
Hydrosphere platform. We will start from scratch and create a simple
linear regression model that will fit our randomly generated data, with
some noise added to it. After the training step, we will pack the model,
deploy it to the platform and invoke it locally with a sample client.

{% hint style="info" %} You can find other examples of how to deploy various models in
our
[examples repository](https://github.com/Hydrospheredata/hydro-serving-example).
{% endhint %}

## Before you start

We assume you already have a @ref[deployed](../install/platform.md)
instance of the Hydrosphere platform and a @ref[CLI](../install/cli.md)
on your local machine.

To let `hs` know where the Hydrosphere platform runs, configure a new
`cluster` entity.

```sh
$ hs cluster add --name local --server http://localhost
$ hs cluster use local
```

## Training a model

We can now start working with the linear regression model. It is a
fairly simple model that fits a randomly generated regression data with
some noise added to it. For data generation we will use the
`sklearn.datasets.make_regression`
([link](https://scikit-learn.org/stable/modules/generated/sklearn.datasets.make_regression.html))
method. We will also normalize data to the [0, 1] range. The model will
be built using [Keras](https://keras.io/) library with
[Tensorflow](https://www.tensorflow.org/) backend.

First of all, create a directory for the model and add `model.py` in to
it.

```sh
$ mkdir linear_regression
$ cd linear_regression
$ touch model.py
```

The model will consist of 3 fully-connected layers, with the first two
having ReLU activation function and 4 units, and the third being a
summing unit with linear activation. Put the following code in your
`model.py` file.

Python : @@snip [train.py](snippets/quickstart/train.py)

We have not yet installed the necessary libraries for our model. In your
`linear_regression` folder, create a `requirements.txt` file with the
following contents:

@@snip [requirements.txt](snippets/quickstart/requirements.txt)

Install all dependencies to your local environment.

```sh
$ pip install -r requirements.txt
```

Train the model.

```sh
$ python model.py
```

As soon as the script finishes, you will get a model saved to a
`model.h5` file.

## Model preparation

Every model in a cluster is deployed as an individual container. After a
request is sent from the client application, it is passed to the
appropriate Docker container with your model deployed on it. An
important detail is that all model files are stored in the
`/model/files` directory inside the container, so we will look there to
load the model.

To run this model we will use a Python runtime that can execute any
Python code you provide. Preparation of a model is pretty
straightforward, though you have to follow some rules:

1. Stick to the specific folder structure to let `hs` parse and upload
   it correctly;
2. Provide necessary dependencies with the `requirements.txt`;
3. Provide a contract file to let the model manager understand the
   model's inputs and outputs.

We will begin with the main functional file.

```sh
$ mkdir src
$ cd src
$ touch func_main.py
```

Hydrosphere communicates with the model using
[TensorProto](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor.proto)
messages. If you want to perform a transformation on the received
TensorProto message, you will have to retrieve its contents, perform a
transformation on them and pack the result back to the TensorProto
message. To do that you have to define a function that will be invoked
every time Hydrosphere handles a request and passes it to the model.
Inside that function, you have to call a `predict` (or similar) method
of your model and return your predictions.

Python : @@snip[serve.py](snippets/quickstart/serve.py)

We do initialization of the model outside of the serving function so
this process will not be triggered every time a new request comes in. We
do that on step (0). The serving function `infer` takes the actual
request, unpacks it (1), makes a prediction (2), packs the answer back
(3) and returns it (4). There is no strict rule for function naming, it
just has to be a valid Python function name.

If you're wondering how Hydrosphere will understand which function to
call from `func_main.py` file, the answer is that we have to provide a
__contract__. A contract is a file that defines the inputs and the
outputs of the model, a signature function and some other metadata
required for serving. Go to the root directory of the model and create a
`serving.yaml` file.

```sh
$ cd ..
$ touch serving.yaml
```



```yaml
kind: Model
name: linear_regression
runtime: "hydrosphere/serving-runtime-python-3.7:$project.released_version$"
install-command: "pip install -r requirements.txt"
payload:
  - "src/"
  - "requirements.txt"
  - "model.joblib"

contract:
  name: infer
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



Here you can see that we have provided a `requirements.txt` and a
`model.h5` as payload files to our model.

The overall structure of our model now should look like this:

```sh
linear_regression
├── model.h5
├── model.py
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

{% hint style="info" %} Although we have `model.py` inside the directory, it will not
be uploaded to the cluster since we did not specify it in the contract's
payload. {% endhint %}

## Serving the model

Now we can upload the model. Inside the `linear_regression` directory,
execute the following command:

```sh
$ hs -v upload
```

Flag `-v` stands for verbose output.

You can open the [http://localhost/models](http://localhost/models) page
to see the uploaded model.

Once you have opened it, you can create an __application__ for it.
Basically, an application represents an endpoint to your model, so you
can invoke it from anywhere. To learn more about advanced features, go
to the [Applications](concepts/applications.md) page.

![](.../linear_regression_application.png)

Open [http://localhost/applications](http://localhost/applications) and
press the `Add New Application` button. In the opened window select the
`linear_regression` model, name your application `linear_regression` and
click the creation button.

If you cannot find your newly uploaded model and it is listed on your
models' page, it is probably still in the building stage. Wait until the
model changes its status to `Released`, then you can use it.

## Invoking an application

Invoking applications is available via different interfaces.

### Test request

You can perform a test request to the model from the Hydrosphere UI.
Open the desired application and press the `Test` button. Internally
it'll mock input data based on the model's contract and send an HTTP
request to the application's endpoint.

### HTTP request

You can use HTTP API to get responses from your Application. Deployed
applications are available for POST requests on the
`{CLUSTER_URL}/gateway/application/{APP_NAME}` endpoint.

Send an HTTP `POST` request.

```sh
$ curl --request POST --header 'Content-Type: application/json' --header 'Accept: application/json' \
    --data '{"x": [[1, 1],[1, 1]]}' 'http://localhost/gateway/application/linear_regression'
```

For more information about invoking applications, refer to this
[page](../how-to/invoke-applications.html).

### gRPC via Python SDK

Define a gRPC client on your side and make a call from it.

Python : @@snip[client.py](snippets/quickstart/client.py)
