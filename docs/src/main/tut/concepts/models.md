---
layout: docs
title:  "Models"
permalink: 'models.html'
---

# Models 

__Model__ is a machine learning model or a processing function that consume provided inputs and produce predictions/transformations. Each model is a collection of its own versions. Every time you upload or re-upload a model, a new version is getting created and added to the collection. At the lowest level model version represented as a Docker image, created based on the model binaries. This essentially means, that during building stage the model version gets frozen and can no longer change. Each collection is identified by the model's name. 

When you upload a model to ML Lambda, roughly the following steps are executed:

1. CLI uploads model binaries to the ML Lambda;
1. ML Lambda builds a new Docker image based on the uploaded binaries and saves it in the configured Docker registry;
1. A builded image is assigned to the model's collection with an increased version.

## Frameworks

Model can be written using a variety of modern machine learning frameworks. You can implement your model using TensorFlow graph computations, or create your model with scikit-learn package, Pytorch, Keras, fastai, MXNet, Spark ML/MLlib, etc. ML Lambda can understand your models depending on what framework you are using. It’s possible due to the metadata, that frameworks save with the model, but it’s not always the case. You should refer to the table below with listed frameworks and their inference. If inferring percentage is high, you can omit providing contracts, otherwise you should.

| Framework | Status | Inferring | Commentaries |
| ------- | ------ | --------- | ------------ |
| TensorFlow | `maintained` | 100% | TensorFlow saves all needed metadata with `SavedModelBuilder`, so generated contracts will be very accurate.  |
| Spark | `partly` | 50% | Spark has metadata, but it's insufficient and contract's inference may be inaccurate. To give an example:<br>1) there isn't enough notation on how shape of the model is formed (i.e. [30, 40] might be a matrix 30x40 or 40x30);<br>2) types are not always coincide with what ML Lambda knows, etc. |
| MXNet | `manual` | 0% | MXNet has it's own export mechanism, but it does not contain any metadata related to types and shapes. |
| SkLearn | `manual` | 0% | Exported models does not provide required metadata. |
| Theano | `manual` | 0% | Exported models does not provide required metadata. |
| ONNX | `manual` | 80% | Currently ML Lambda is able to read ONNX's proto files, but due to the lack of support from other frameworks (PyTorch, TensorFlow, etc.) ONNX models cannot be run in the implemented runtimes. | 

<p style="font-size:0.8em; margin-top: 10px; margin-left: 8px;">
	<code>maintained</code> - No need to provide any self-written contracts, model definitions;
	<code>partly</code> - Complicated models will likely fail inference;
	<code>manual</code> - Need to provide self-written contracts, model definitions.
</p>

<br>
<hr>

# What's next?

- [Learn more about application]({{site.baseurl}}{%link concepts/applications.md%})
- [Learn how to invoke applications]({{site.baseurl}}{%link how-to/invoke-applications.md%})