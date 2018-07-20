---
layout: docs
title:  "Models"
permalink: 'models.html'
---

# Machine Learning Model Management

## Principles 

The whole process of working with ML Lambda consits of working with your models and deploying them via [applications]({{site.baseurl}}{%link applications.md%}) to production. Here we describe the main principles with working with models. 

### Models

__Models__ are machine learning models or processing functions, that consume provided inputs and produce predictions. 

When you upload model binaries to ML Lambda, the following steps are automatically executed:
1. Check, if the model has been provided with the [contract]({{site.baseurl}}{%link models.md%}#contracts). If so, generate binary contract representation based on the provided contract; 
2. If contract hasn't been found, extract model's metadata (signatures, types) and generate binary contract from it;
3. Build a Docker image with the model and place all models data inside;
4. Increment model's version and assign it with the created Docker image;
5. Push image to the registry. 

#### Models' Versioning

ML Lambda automatically recognizes new versions of deployed models, and then uses docker registry to save new immutable docker image with new model version. Later this docker image is used as an init container for the Runtime during actual deployment.

![]({{site.baseurl}}{%link /img/ui-models-versions.png%})<br>
<span style="font-size: 0.95em; margin-top: 10px;">__Pic.1__ ML Lambda tracks the history of model's updates and preserves it in the model's __versions__ section.</span>

![]({{site.baseurl}}{%link /img/ui-models-pick.png%})<br>
<span style="font-size: 0.95em; margin-top: 10px;">__Pic.2__ When configuring application, you can pick any desired version of any model and use it inside pipeline.</span>

#### Frameworks

ML Lambda can understand your models depending on what framework you're using. It's possible due to the metadata that popular frameworks (such as TensorFlow) save with the model. But it's not always the case. You should refer to the table below with listed frameworks and their inferrence. If inferring percentage is high, you can omit providing self-written [contracts]({{site.baseurl}}{%link models.md%}#contracts), otherwise you should.

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

If your models is not supported, or inferrence percentage is low, you may need to describe the model by yourself. To do that you should use __contracts__. Contract is a concept, that allows you to decsribe model's inputs and outputs. To describe your model, you would have to provide 2 following documents. 

##### serving.yaml

The file provides the definition of the model.

```yaml
model:
  name: "linear_regression"
  type: "keras:2.2.0"
  contract: "contract.prototxt"
  payload:
    - "src/" 
    - "model.h5"
```

| Field | Definition |
| ----- | ---------- |
| `name` | Specifies the name of the model. |
| `type` | Specifies the type and the version of the model, e.g. `python:3.6.5`, `tensorflow:1.8.0`, `scikit-learn:0.19.1`, etc. Should be of format `<framework>:<version>`. |
| `contract` | Filename of the contract of the model. ML Lambda will look up this file to infer the contract. |
| `payload` | List of all files, used by model. If model is organized with directires/subdirectories, it's enough to only specify root directory. ML Lambda will recoursively add every file/subdirectory inside it. |

<br>

##### contract.prototxt

The file provides the information about your signatures. It closely resembles our defined [ModelSignature][github-model-signature].

```
signatures {
    signature_name: "serving"
	inputs {
	    name: "profile"
		shape: {
		    dim: {
			    size: 3
			}
		}
		dtype: DT_DOUBLE
	}
    inputs {
	    name: "profile_context"
		shape: {
		    dim: {
			    size: 4
			}
			dim: {
			    size: 12
			}
		}
		dtype: DT_DOUBLE
	}
    outputs {
	    name: "user_class"
		subfields {
		    data {
			    name: "user_name"
				dtype: DT_STRING
			}
		}
	}
    outputs {
	    name: "obvserver_class"
		dtype: DT_DOUBLE
	}
    outputs {
	    name: "misc"
		shape: {
		    dim: {
			    size: -1
			}
		}
		dtype: DT_DOUBLE
	}
}
```

| Field | Links | Explanation |
| ----- | --------- | ---------- |
| `inputs`| [ModelFields][github-model-field] | The field is a mapping, which will be provided to the model. The key of the mapping would be the `name` of the field, the value would be actual data.<br>If you want to provide a dictionary with multiple key inputs, simply define another field `inputs` in the signature definition. |
| `outputs`| [ModelFields][github-model-field] | The field is a mapping, which will be expected from the model. The key of the mapping would be the `name` of the field, the value would be actual data.<br>If you want to get a dictionary with multiple key outputs, simply define additional `outputs` in the signature definition. |
| `shape` | [TensorShape][github-tensor-shape] | Describes the shape of the tensor. If you want to specify multidimensional shape, you need to provide multiple `dim` definitions. If you don't know the size of the shape, you can assign `size` to `-1`. ML Lambda will expect 0, 1 or many objects in that case.|
| `dtype` | [DataType][github-datatype] | Can be one of the following values:<br>`DT_INT8`, `DT_INT16`, `DT_INT32`, `DT_INT64`;<br>`DT_UINT8`, `DT_UINT16`, `DT_UINT32`, `DT_UINT64`;<br>`DT_QINT8`, `DT_QINT16`, `DT_QINT32`;<br>`DT_QUINT8`, `DT_QUINT16`;<br>`DT_HALF`, `DT_FLOAT`, `DT_DOUBLE`;<br>`DT_COMPLEX64`, `DT_COMPLEX128`;<br>`DT_BOOL`, `DT_STRING`;<br>`DT_MAP` |

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
        tags=['foo_tag', 'bar_tag'],
        signature_def_map=foo_signatures
    )

builder.save()
```

Upload model and create a corresponding application that uses `hydrosphere/serving-runtime-tensorflow` as runtime. 

```sh
$ cd saved_model
$ hs upload --host "localhost" --port 8080
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
        inputs={'input': model.input}, outputs={'output': model.output}
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
    $ hs upload --host "localhost" --port 8080
    ```

2. If you're using different backend, you can save this model to `.h5` format and run it under python runtime with pre-defined `requirements.txt`, `contract.prototxt` and `serving.yaml` files. [Here's]({{site.baseurl}}{%link getting-started.md%}#own-model) step-by-step guide on how to achieve that.

### Python

In order to upload and use Python model, you will need to do the following:

1. Create dedicated folder.
2. Create a `func_main.py`, wich will handle your interactions with the model and other processing steps. Put this file inside `/model/src/` direcotory. 
3. Define `contract.prototxt` ([Reference]({{site.baseurl}}{%link models.md%}#contractprototxt)). Remember, you should define signature with the name, that will match to one of your function's name in `func_main.py`. 
4. Define `serving.yaml` ([Reference]({{site.baseurl}}{%link models.md%}#servingyaml)).

The directory structure should look like:
```
model
├── contract.prototxt
├── serving.yaml
├── ...
└── src
    └── func_main.py
```

Now upload model and create a correspnding application that uses `hydrosphere/serving-runtime-python` as runtime.

```sh
$ hs upload --host "localhost" --port 8080
```

Create application for that model and select `hydrosphere/serving-runtime-python` runtime. You're all set.

[Here's]({{site.baseurl}}{%link getting-started.md%}#own-model) step-by-step guide describing, how to run Keras model inside python runtime conteiner.


[github-model-signature]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_signature.proto
[github-model-field]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_field.proto
[github-tensor-shape]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor_shape.proto
[github-datatype]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/types.proto