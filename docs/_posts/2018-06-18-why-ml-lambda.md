---
layout: post
title:  "Why ML Lambda?"
date:   2018-06-15 13:57:31 +0300
permalink: 'why-ml-lambda.html'
---


Have you ever questioned yourself if the work of building ml-models and deploying
them to the cloud was quite simple or a little exhausting? Maybe not a little? 

Deploying small scikit-learn models might be the easiest one to mention.
You just spin up an HTTP-server, initiate a model, define a handler for calling
`predict()` method and you're all set. Performance wise it will satisfy most of
the use cases. TensorFlow serving is a bit more complicated but feasible as
well.

Although this might be a solution, it will greatly reduce
configuration/deployment time when the case becomes more complicated. In real
projects you would have to deploy pipelines of different models and custom
functions rather than one simple model. Locking yourself under strict limits to
a certain amount of technologies or computational power is quite unreasonable.
Allocating additional time on devOps type of job to just make your models work
doesn't seem like an effective way to use the time. Data scientists need to
`build` → `test` → `deploy` models to production quickly and easily. Not to
mention that this should work continuously.

Assume you have to build a natural language processing pipeline that takes
JSON-file as an input from web application, extracts simple feautres,
classifies them into particular category then for that particular category
picks a pre-trained neural network, applies it and returns the output. 

![]({{site.baseurl}}{%link /assets/example-pipeline.png%})

Now imagine you've decided to optimize your pipeline and embedded word2vec as
first two layers in a new DL4J neural network. To analyze the performance of the
new solution you decided to split your traffic and let 10% of the traffic flow
through your new NN. Your production experiment now should look like this. 

![]({{site.baseurl}}{%link /assets/example-pipeline-modified.png%})
