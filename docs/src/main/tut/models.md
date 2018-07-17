---
layout: docs
title:  "Models"
permalink: 'models.html'
position: 4
---

# Models

__Models__ are machine learning models or processing functions, that consume provided inputs and produce predictions. When you upload model binaries to ML Lambda, it will automatically execute following steps:
1. Check, if the model has been provided with the [contract]({{site.baseurl}}{%link contracts.md%}). If so, generate binary contract representation based on the provided contract; 
2. If contract hasn't been found, extract model's metadata (signatures, types) and generate binary contract from it;
3. Build a Docker image with the model and place all models data inside `/model/files/` directory. Place generated binary contract `contract.protobin` inside `/model/` directory;
4. Increment model's version and assign it with the created Docker image;
5. Push image to the registry. 

You can look up all essential details from _Models_ page.

| Field | Description |
| ----- | ----------- |
| Version | Current version of the model. |
| Created | Date, when model was _built_. |
| Build Status | Refers to the current build status of the model. Can be on of the following: `Pending`, `Running`, `Failed`, `Finished`. |
| Applications | List of applications, where current model's version is used. |
| Model Type | Type of the model, specified in `serving.yaml` | 

<br>
_Note: If you want to revise models' contracts, you can follow right arrow icon on the interface._

![]({{site.baseurl}}{%link /img/ui-models-page.png%})

## Uploading Models

Below you can find differnet ways to upload your models.

### Uploading TensorFlow

Running tensorflow models with ML Lambda is pretty simple, just save you model and it's variables with `SavedModelBuilder` and you're ready to go. 

```
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

### Uploading Keras

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

2. If you're using different backend, you can save this model to `.h5` format and run it under python runtime with pre-defined `requirements.txt`, `contract.prototxt` and `serving.yaml` files. [Here's]({{site.baseurl}}{%link getting-started.md%}#uploading-own-model) step-by-step guide on how to achieve that.

### Uploading Python

In order to upload and use Python model, you will need to do the following:

1. Create dedicated folder.
2. Create a `func_main.py`, wich will handle your interactions with the model and other processing steps. Put this file inside `/model/src/` direcotory. 
3. Define `contract.prototxt` ([Reference]({{site.baseurl}}{%link contracts.md%}#contractproto)). Remember, you should define signature with the name, that will match to one of your function's name in `func_main.py`. 
4. Define `serving.yaml` ([Reference]({{site.baseurl}}{%link contracts.md%}#servingyaml)).

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

Create application for that model and select `hydrosphere/serving-runtime-python` runtime.