---
layout: docs
title:  "Models"
permalink: 'models.html'
position: 4
---

__Models__ are machine learning models or processing functions, that consume provided inputs and produce predictions. When you upload model binaries to ML Lambda, it will automatically execute the following steps:
1. Checks, if the model has been provided with the [contract]({{site.baseurl}}{%link contract.md%}). If so, it generates binary contract representation based on the provided contract; 
2. If contract hasn't been found, it extracts model's metadata (signatures, types) and generates binary contract from it;
3. Builds a Docker image with the model and places all models data inside `/model/files/` directory. Generated binary contract `contract.protobin` is places inside `/model/` directory;
4. Increments model's version and assigns it with the created Docker image;
5. Pushes image to the registry. 

New version will be generated each time you execute the following code inside your model's folder. 

```sh
$ hs upload --host "localhost" --port 8080
```

You can look up all essential details in [http://127.0.0.1/models](http://127.0.0.1/models) page.

| Field | Description |
| ----- | ----------- |
| Version | Current version of the model. |
| Created | Date, when model was _built_. |
| Build Status | Refers to the current build status of the model. Can be on of the following: `Pending`, `Running`, `Failed`, `Finished`. |
| Applications | List of applications, where current model's version is used. |
| Model Type | Type of the model, specified in `serving.yaml` | 

If you want to revise models' contracts, you can follow right arrow icon on web-interface. 

![]({{site.baseurl}}{%link /img/ui-models-page.png%})
<sup>__Pic. 1__: Models page.</sup>