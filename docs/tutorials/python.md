# Serving Python model

A Python model is a model that is backed with a Python runtime. You can create a Python model to perform any transformation. Simply provide an execution script packed with a serving function. In this tutorial we will create a simple increment function.

## Before you start

We assume you already have a @ref[deployed](../install/platform.md) instance of the Hydrosphere platform and a @ref[CLI](../install/cli.md) on your local machine.

To let `hs` know where the Hydrosphere platform runs, configure a new `cluster` entity.

```bash
$ hs cluster add --name local --server http://localhost
$ hs cluster use local
```

## Model structure

First of all, let's create a directory where we will put all of our code:

```bash
$ mkdir -p increment_model/src
$ cd increment_model
$ touch src/func_main.py
```

{% hint style="info" %}
Generally, we use `hydrosphere/serving-runtime-python-3.7` runtime for serving Python models. This runtime uses `src/func_main.py` script as an entry point. You may create any arbitrary Python application within your model, just keep in mind that the entry point of your script has to be in `src/func_main.py`.
{% endhint %}

## Model dependencies

By default the `hydrosphere/serving-runtime-python-3.7` runtime does not have any scientific packages pre-installed, you will have to manage this yourself. Let's add `requirements.txt`:

```bash
$ echo "tensorflow==2.2\nnumpy==1.19" > requirements.txt
```

## Serving function

Now let's implement the serving function, which will handle requests. Open `src/func_main.py` and paste the following code:

Python : @@snip[serve.py](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/tutorials/serving/snippets/python/serve.py)

A `number` argument is a Numpy float scalar.

After all computations are performed, we need to return a dictionary with an output value.

{% hint style="info" %}
If you use some external file \(for example, such as @ref[model's weights](../getting-started/serving-simple-model.md#model-preparation)\), you would have to specify the absolute path to that file. By default, all files that you specify in the contract, are placed in the `/model/files` directory inside the runtime.
{% endhint %}

## Model definition

To help Hydrosphere understand, what the inputs and the outputs of your model are, you have to provide a definition of that model. Create a `serving.yaml` file inside your folder root.

```yaml
kind: Model
name: "increment_model"
runtime: "hydrosphere/serving-runtime-python-3.7:$project.released_version$"
install-command: "pip install -r requirements.txt"   # this line will be executed during model build
payload:   # define all files of your model that has to be packed
  - "src/"
  - "requirements.txt"

contract:
  name: increment  # name of signature is the name of the serving function
  inputs:
    number:
      shape: scalar
      type: int64
      profile: numerical
  outputs:
    number:
      shape: scalar
      type: int64
      profile: numerical
```

{% hint style="info" %}
If `.contract.name` is not set, it defaults to `predict`.
{% endhint %}

'serving.yaml' file describes the model, its name, type, payload, and contract. The model contract declares input and output fields' data types and shapes. Fields' profile information is used to tell Hydrosphere how to interpret this field.

That's it, you've just created a model that can be used within your business application.

## Model deployment

Upload the model to the Hydrosphere platform.

```bash
$ hs upload
```

Once the model has been uploaded, it can be exposed for serving as an application.

Create an application to declare an endpoint to your model. You can create it manually via Hydrosphere UI, or by providing an application manifest.

```bash
$ hs apply -f - <<EOF
kind: Application
name: increment_app
singular:
  model: increment_model:1
EOF
```

## Prediction

You can send requests to your models using gRPC or HTTP endpoints, e.g.

```bash
$ curl --request POST --header 'Content-Type: application/json' --header 'Accept: application/json' \
    --data '{ "number": [1] }' 'http://<host>/gateway/application/increment_app'
```

