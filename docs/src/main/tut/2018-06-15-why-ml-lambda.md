---
layout: docs
title:  "Why ML Lambda?"
date:   2018-06-15
permalink: 'why-ml-lambda.html'
---


Have you ever questioned yourself if the work of building ml models and deploying them to the cloud was quite simple or a little exhausting? Maybe not a little? Deploying small _scikit-learn_ models might be the easiest one to mention. You just spin up an HTTP-server, initiate a model, define a handler for calling `predict()` method and you're all set. Performance wise it will satisfy most of the cases. TensorFlow serving is a bit more complicated but feasible as well.

Although this might be a solution, it will greatly reduce configuration/deployment time when the case becomes more complicated. In real projects you would have to deploy pipelines of different models and custom functions rather than one simple model. Locking yourself under strict limits to a certain amount of technologies or computational power is unreasonable. Allocating additional time on devOps type of job to just make your models work cannot be an effective way to use time. Data scientists need to `build → test → deploy` models to production quickly and easily. Not to mention that this should also work continuously.

# Example

1. Assume you have to build up a natural language processing pipeline that takes JSON-file as an input from a web application, extracts simple feautres, classifies them into particular category, then, for that particular category, picks a pre-trained neural network, applies it and returns the output. 

	![]({{site.baseurl}}{%link /img/example-nlp-pipeline.png%})
	<sup>__Pic. 1__: Example NLP pipeline.</sup>

2. Now imagine you've decided to optimize your pipeline and embedded word2vec as first two layers in a new NN. To analyze the performance of the new solution you've decided to split your traffic and let 10% of the traffic flow through your new NN. Your production experiment now should look like the following. 

	![]({{site.baseurl}}{%link /img/example-nlp-pipeline-modified.png%})
	<sup>__Pic. 2__: Example NLP pipeline. Modified NN.</sup>

3. Another amazing idea came to your mind and you've decided to transform your serving pipeline to work in a streaming context (i.e. deploy a prediction pipeline into Apache Kafka).
	
	![]({{site.baseurl}}{%link /img/example-nlp-pipeline-modified-2.png%})
	<sup>__Pic. 3__: Example NLP pipeline. Inserted in the Kafka streaming context.</sup>

Have you already imagined the whole architecture in your mind? How many hours would you spend on configuring this? 10 hours? 20? 100? There's an option to ask engineers to re-implement it in Java/Spark which can take a couple more months and an unpredictable outcome. But there's a much simplier way. 


# Solution

ML Lambda manages multiple serverless runtimes and allows chaining them into end-to-end serving pipelines. Monitoring, versioning, auto-scalling, models auto discovery and user friendly interface goes on top. It implements side-car architecture that decouples machine learning logic from networking, service dicsovery, load balancing. So, the trasport layer between model runtimes could be changed from HTTP to Unix sockets and even to Kafka without changing containers with actual machine learning code. 