---
layout: docs
title:  "Models"
permalink: 'models.html'
---

# Machine Learning Model Management

## Principles 

The whole process of working with ML Lambda consits of working with your models and deploying them via [applications]({{site.baseurl}}{%link applications.md%}) to production. Here we describe the main principles of working with the models. 

### Models

__Models__ are machine learning models or processing functions, that consume provided inputs and produce predictions. 

When you upload model binaries to ML Lambda, the following steps are automatically executed:
1. Check, if the model has been provided with the [contract]({{site.baseurl}}{%link models.md%}#contracts). If so, generate binary contract representation based on the provided contract; 
2. If contract hasn't been found, extract model's metadata (signatures, types) and generate binary contract from it;
3. Build a Docker image of the model and add it to the configured Docker registry;
4. Increment model's version and assosiate it with the created Docker image;

#### Models' versioning

ML Lambda automatically recognizes new versions of deployed models and uses Docker registry to save there new immutable model versions. Later these Docker images will be used to build runtime containers during actual deployment.

![]({{site.baseurl}}{%link /img/ui-models-versions.png%})<br>

#### Frameworks

ML Lambda can understand your models depending on what framework you are using. It's possible due to the metadata that popular frameworks (such as TensorFlow) save with the model, but it's not always the case. You should refer to the table below with listed frameworks and their inferrence. If inferring percentage is high, you can omit providing self-written [contracts]({{site.baseurl}}{%link models.md%}#contracts), otherwise you should.

| Framework | Status | Inferring | Commentaries |
| ------- | ------ | --------- | ------------ |
| TensorFlow | `maintained` | 100% | TensorFlow saves all needed metadata with `SavedModelBuilder`, so generated contracts will be very accurate.  |
| Spark | `partly` | 50% | Spark has metadata, but it's insufficient and contract's inference may be inaccurate. To give an example:<br>1) there isn't enough notation on how shape of the model is formed (i.e. [30, 40] might be a matrix 30x40 or 40x30);<br>2) types are not always coincide with what ML Lambda knows, etc. |
| MXNet | `manual` | 0% | MXNet has it's own export mechanism, but it does not contain any metadata related to types and shapes. |
| SkLearn | `manual` | 0% | Exported models does not provide required metadata. |
| Theano | `manual` | 0% | Exported models does not provide required metadata. |
| ONNX | `on hold` | 80% | Currently ML Lambda is able to read ONNX's proto files, but due to the lack of support from other frameworks (PyTorch, TensorFlow, etc.) ONNX models cannot be run in the implemented runtimes. | 

<p style="font-size:0.8em; margin-top: 10px; margin-left: 8px;">
	<code>maintained</code> - No need to provide any self-written contracts, model definitions.<br>
	<code>partly</code> - Complicated models will likely fail inference.<br>
	<code>manual</code> - Need to provide self-written contracts, model definitions.<br>
	<code>on hold</code> - Full support is postponed.
</p>

### Contracts

If your models is not supported or inferrence percentage is low, you may need to describe the model by yourself. To do that you should use __contracts__. Contract is a concept that allows you to decsribe model's inputs and outputs. 

```yaml
kind: Model
name: "example_tokenizer_model"
model-type: "python:3.6"
payload:
  - "src/"
  - "requirements.txt/"
  - "nltk_data"
  
contract:
  tokenize:                 # arbitrary: name of the signature
    inputs:                 
      input_text:           # arbitrary: name of the field
        shape: [-1, 1]
        type: string
        profile: text
    outputs:                
      output_text:          # arbitrary: name of the field
        shape: [-1, 1]
        type: string
        profile: text
```

`model-type` specifies the type and the version of the model, e.g. `python:3.6.5`, `tensorflow:1.8.0`, `scikit-learn:0.19.1`, etc. Should be of format `<framework>:<version>`. 

`contract` might consist of multiple signatures with different names. The choice of name depends only on you. Later on in the web interface you can choose between defined signatures in your applications. `inputs` and `outputs` share the same annotation. 

| Field | Definition | Type | Type description |
| ----- | ---------- | ---- | ---------------- |
| `shape` | Describes the shape of the input/ouput tensor. | `-1` | Indicates arbitrary shape. 
| | | `1`, `2`, `3`, ... | Any positive number |

<br>

| Field | Definition | Type | Type description |
| ----- | ---------- | ---- | ---------------- |
| `type` |  Describes the data type of incoming tensor. | `half` | 16-bit half-precision floating-point |
| | | `float`       | 32-bit single-precision floating-point | 
| | | `double`      | 64-bit double-precision floating-point | 
| | | `bfloat16`    | 16-bit truncated floating-point |
| | | `complex64`   | 64-bit single-precision complex | 
| | | `complex128`  | 128-bit double-precision complex | 
| | | `uint8`       | 8-bit unsigned integer | 
| | | `uint16`      | 16-bit unsigned integer | 
| | | `uint32`      | 32-bit unsigned integer | 
| | | `uint64`      | 64-bit unsigned integer | 
| | | `int8`        | 8-bit signed integer | 
| | | `int16`       | 16-bit signed integer | 
| | | `int32`       | 32-bit signed integer | 
| | | `int64`       | 64-bit signed integer | 
| | | `bool`        | Boolean | 
| | | `string`      | String | 
| | | `qint8`       | Quantized 8-bit signed integer | 
| | | `quint8`      | Quantized 8-bit unsigned integer | 
| | | `qint16`      | Quantized 16-bit signed integer | 
| | | `quint16`     | Quantized 16-bit unsigned integer | 
| | | `resource`    | Handle to a mutable resource | 
| | | `variant`     | Values of arbitrary types |

<br>

| Field | Definition | Type | Type description |
| ----- | ---------- | ---- | ---------------- |
| `profile` | Describes the nature of the data. | `text` | For monitoring such fields will be used __text__-oriented algorithms. |
| | | `image` | For monitoring such fields will be used __image__-oriented algorithms. | 
| | | `numerical` | For monitoring such fields will be used __numerical__-oriented algorithms.|

## Examples

Here we listed methods on how to work with different models.

* [TensorFlow]({{site.baseurl}}{%link models.md%}#tensorflow)
* [Keras]({{site.baseurl}}{%link models.md%}#keras)
* [Python]({{site.baseurl}}{%link models.md%}#python)

### TensorFlow

Running tensorflow models with ML Lambda is pretty simple, just save your model and its variables with `SavedModelBuilder` and you're ready to go. 

```python
import tensorflow as tf

...
builder = tf.saved_model.builder.SavedModelBuilder('saved_model')

with tf.Session() as sess:
    ...
    builder.add_meta_graph_and_variables(
        sess=sess,
        tags=[tf.saved_model.tag_constants.SERVING],
        signature_def_map=foo_signatures
    )

builder.save()
```

Upload model and create a corresponding application that uses `hydrosphere/serving-runtime-tensorflow` as runtime. 

```sh
$ cd saved_model
$ hs upload
```

### Keras

There're 2 ways to run Keras models with ML Lambda.

1. If you're using `tensorflow` backend, you can run Keras model using `tensorflow` runtime. To do that, you'll need to export your model with `SavedModelBuilder`.

    ```python
    from keras import backend
    import tensorflow as tf

    # create and train model
    model = ...

    # export trained model
    builder = tf.saved_model.builder.SavedModelBuilder('saved_model')
    signature = tf.saved_model.signature_def_utils.predict_signature_def(
        inputs={'input': model.inputs}, outputs={'output': model.outputs}
    )
    builder.add_meta_graph_and_variables(
        sess=backend.get_session(),
        tags=[tf.saved_model.tag_constants.SERVING],
        signature_def_map={
            tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY: signature
        }
    )

    # save model
    builder.save()
    ```

    Upload model and create a corresponding application that uses `hydrosphere/serving-runtime-tensorflow` as runtime. 

    ```sh
    $ cd saved_model
    $ hs upload
    ```

2. If you're using different backend, you can save this model to `.h5` format and run it under python runtime with pre-defined `requirements.txt` and `serving.yaml` files. [Here's]({{site.baseurl}}{%link getting-started.md%}#own-model) step-by-step guide on how to achieve that.

### Python

In order to upload and use Python model, you will need to do the following:

1. Create dedicated folder.
2. Create a `func_main.py`, wich will handle your interactions with the model and other processing steps. Put this file inside `/model/src/` direcotory. 
3. Define `serving.yaml` ([Reference]({{site.baseurl}}{%link models.md%}#servingyaml)). Remember, you should define signature with the name, that will match to one of your function's name in `func_main.py`. 

The directory structure should look like:
```
model
├── serving.yaml
├── ...
└── src
    └── func_main.py
```

Now upload model and create a correspnding application that uses `hydrosphere/serving-runtime-python` as runtime.

```sh
$ hs upload
```

Create application for that model and select `hydrosphere/serving-runtime-python` runtime. You're all set.

[Here's]({{site.baseurl}}{%link getting-started.md%}#own-model) step-by-step guide describing, how to run Keras model inside python runtime container.


[github-model-signature]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_signature.proto
[github-model-field]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_field.proto
[github-tensor-shape]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor_shape.proto
[github-datatype]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/types.proto