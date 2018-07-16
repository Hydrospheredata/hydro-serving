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

* [TensorFlow]({{site.baseurl}}{%link models.md%}#tensorflow)
* [Keras]({{site.baseurl}}{%link models.md%}#keras)
* [Python]({{site.baseurl}}{%link models.md%}#python)
* [Apache Spark]({{site.baseurl}}{%link models.md%}#apache-spark)

### TensorFlow
Todo

### Keras

You can run Keras models using `hydrosphere/serving-runtime-tensorflow` runtime. To do that, you'll need to export your model with `SavedModelBuilder`.

### Python

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

Now upload model.
```sh
$ hs upload --host "localhost" --port 8080
```

Create application for that model and select `hydrosphere/serving-runtime-python` runtime.