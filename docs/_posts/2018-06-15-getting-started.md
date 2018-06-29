---
layout: post
title:  "Getting Started"
date:   2018-06-15
permalink: 'getting-started.html'
---

This document presents a quick way to set everything up and deploy your first model.


# Installation
{: #installation}

## ML Lambda

To get started, make sure, you have installed [Docker][docker-install] on your machine and clone ML Lambda into desired directory.

>Note: __hydro-serving__ is the working name of ML Lambda. 

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```

Now set up a docker environment. You have 2 options:

1. Lightweight version that doesn't contain any metrics and doesn't support Kafka. It will only allow you to deploy and run your models in a continuous manner. 

	```sh
	$ cd ./hydro-serving/
	$ docker-compose up --no-start
	```

2. Full version of ML Lambda with integrations to Kafka, Graphana, different metrics, etc. This might take a while. 
	```sh
	$ cd ./hydro-serving/integrations/
	$ docker-compose up --no-start
	```

>Note: If you've already installed one of the versions and want to install the other one, you may need to clean existing containers with `docker container rm $(docker container ls -aq)`.

After all images will be pulled, start ML Lambda..

```sh
$ docker-compose up
```
...and go the web-interface [http://127.0.0.1/][ml-lambda]. If everything was fine, you should see this: 


## CLI

ML Lambda has a great [cli-tool][hydro-serving-cli], that lets you easily upload models. It's supports Python 3.4 and above. To install, run:

```sh
$ pip install hs
```

For more details about on how to use CLI, you can look up specified section. 

# Running models

To get the notion of ML Lambda we recommend you to go through 2 bellow processes of running demo models and deploying your own models. This will show you different aspects of working with CLI, configuring model, etc. 

## Running demo
We've already created a few different [examples][hydro-serving-examples] that you can run to see, how everything works. Let's clone them and pick a models. 

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-example
$ cd ./hydro-serving-examples/models/$MODEL_OF_YOUR_CHOICE
```

For the purpose of this tutorial we chose [stateful LSTM][stateful-lstm]. All you have to do is just to upload the model to the server. 

> Note: At this stage you must start ML Lambda with `docker-compose up` in `hydro-serving` folder if you haven't done that yet.

```sh
$ hs upload
```

Additionally Stateful LSTM needs [pre][stateful-lstm-pre] and [post][stateful-lstm-post] processing stages. So we upload them as well.

Your models now have been uploaded to ML Lambda. You can find them here - [http://127.0.0.1/models][models]. Let's go and create an application that can use your models. 

# Running Demo 
{: #running-demo-model}

# Running Own Model 
{: #running-demo-model} -->

[docker-install]: https://docs.docker.com/install/
[docker-hub]: https://hub.docker.com/u/hydrosphere/
[docker-hub-python]: https://hub.docker.com/u/hydrosphere/serving-runtime-python/
[docker-hub-spark]: https://hub.docker.com/r/hydrosphere/serving-runtime-spark/
[docker-hub-tensorflow]: https://hub.docker.com/u/hydrosphere/serving-runtime-tensorflow/

[hydro-sonar]: https://hydrosphere.io/sonar/
[hydro-serving-cli]: https://github.com/Hydrospheredata/hydro-serving-cli
[hydro-serving-examples]: https://github.com/Hydrospheredata/hydro-serving-example
[appache-kafka]: https://kafka.apache.org
[stateful-lstm]: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models/stateful_lstm
[stateful-lstm-pre]: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models/stateful_lstm_preprocessing
[stateful-lstm-post]: https://github.com/Hydrospheredata/hydro-serving-example/tree/master/models/stateful_lstm_postprocessing
[ml-lambda]: http://127.0.0.1/
[applications]: http://127.0.0.1/applications
