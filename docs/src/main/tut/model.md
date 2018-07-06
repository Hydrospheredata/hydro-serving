---
layout: docs
title:  "Models"
permalink: 'models.html'
position: 4
---

__Models__ are entities, that consume provided inputs and produce predictions. When you upload a new model to ML Lambda, it will generate corresponded entity, infer it's [contract]({{site.baseurl}}{%link contract.md%}) and assign a version to it. New version will be generated each time you execute the following code inside your model's folder. 

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