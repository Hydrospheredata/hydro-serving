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

To get started, make sure, you have installed [Docker][docker-install] on your machine.

Clone ML Lambda to the desired directory.

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

After all images will be pulled, start ML Lambda.

```sh
$ docker-compose up
```

Go the web-interface [http://127.0.0.1/][ml-lambda]. If everything was fine, you should see this: 

![]({{site.baseurl}}{%link /assets/ui-hydrosphere-main.png%})
<sup>__Pic. 1__: Web interface.</sup>

## CLI

ML Lambda has a great [cli-tool][hydro-serving-cli], that lets you easily upload models. It supports Python 3.4 and above. To install, run:

```sh
$ pip install hs
```

For more details about on how to use CLI, check specified section. 

# Running models

To get the notion of ML Lambda we recommend you to go through 2 bellow tutorials of running demo models and deploying your own models. This will show you different aspects of working with CLI, configuring model, etc. 

## Running demo
{: #running-demo}

### Uploading Models

We've already created a few different [examples][hydro-serving-examples] that you can run to see, how everything works. Let's clone them and pick a model. 

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-example
$ cd ./hydro-serving-example/models/$MODEL_OF_YOUR_CHOICE
```

For the purpose of this tutorial we chose [stateful LSTM][stateful-lstm]. All you have to do is just to upload models to the server. 

> Note: At this stage you must start ML Lambda with `docker-compose up` in `hydro-serving` folder if you haven't done that yet.

```sh
$ cd ./hydro-serving-example/models/stateful_lstm
$ hs upload
```

Additionally Stateful LSTM needs [pre][stateful-lstm-pre] and [post][stateful-lstm-post] processing stages. So we upload them as well.

```sh
$ cd ../stateful_lstm_preprocessing
$ hs upload
$ cd ../stateful_lstm_postprocessing
$ hs upload
```

Your models now have been uploaded to ML Lambda. You can find them here - [http://127.0.0.1/models][models]. 

### Creating Application

Let's go ahead and create an application that can use our models. Open `Applications` page and press `ADD NEW`. 

Stateful LSTM is actually a __Multi-Staged__ application, and consists of the following parts:

| Stage | Model | Runtime | Description |
| ----- | ----- | ------- | ----------- |
| 1 | demo_preprocessing | hydrosphere/serving-runtime-python:3.6:latest | ... |
| 2 | stateful_lstm | hydrosphere/serving-runtime-tensorflow:1.7:latest | ... |
| 3 | demo_postprocessing | hydrosphere/serving-runtime-python:3.6:latest | ... |

Reproduce described structure in the `Models` framework and create an application. Set _Application Name_ to `demo_lstm`. ML Lambda will automatically detect and fill models' signatures for you.

>Note: There's an option to add multiple models to one stage, which might be confusing, because you may include all pipeline steps(pre, lstm, post) into a signle stage. Adding new stage is performed via `ADD NEW STAGE` button.

### Running Application

After you've created an app you're able to invoke it via different interfaces.

#### Test-request

You can test your application via web-interface. Press `Test` button in the application's page. Test data will be automatically pulled from model's contract. 

#### HTTP-request

Send `POST` request to ML Lambda.

>Note, __demo_lstm__ is the name of your newly created application. If you named it differently, adjust request accordingly.

```sh
$ curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "data": [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1] 
 }' 'http://localhost:8080/api/v1/applications/serve/1/demo_lstm'
```

## Running Own Application
{: #running-own-app}



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
[models]: http://127.0.0.1/models
[applications]: http://127.0.0.1/applications
