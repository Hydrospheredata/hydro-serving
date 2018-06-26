---
layout: post
title:  "Quickstart"
date:   2018-06-15
permalink: 'quickstart.html'
---

This document presents a quick way to set everything up and deploy your first model on various environments.

* [Localhost](#localhost)
* AWS
* Google Cloud Engine


# Localhost
{: #localhost}

To get started, download [Docker][docker-install] on your machine if you don't have one. 

Next, clone ML Lambda (the working name is _hydro-serving_) into desired directory.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```

Now set up a docker environment. You have 2 options: This will take a while. 

1. Lightweight version that doesn't contain any metrics and doesn't support Kafka. It will only allow you to deploy and run your models in a continuous manner. 

	```sh
	$ cd ./hydro-serving/
	$ docker-compose up # --no-start if you don't want to launch an image instanly
	```

2. Full version of ML Lambda with integrations to Kafka, Graphana, different metrics, etc. 
	```sh
	$ cd ./hydro-serving/integrations/
	$ docker-compose up # --no-start if you don't want to launch an image instanly
	```

>Note: If you are already installed one of the versions and want to install the other one, you may need to clean existing containers with `docker container rm $(docker container ls -aq)`.

Now, that you're ready with the docker, let's go ahead and deploy your first model. For this purpose we've built a handful [cli-tool][hydro-serving-cli]. 

```sh
$ pip install hs
```

If you don't have any ready models at the moment, you can use our [examples][hydro-serving-examples] and work with them. 

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-example
$ cd ./hydro-serving-examples/models/$MODEL_OF_YOUR_CHOICE
```

For the purpose of this tutorial we chose [stateful LSTM][stateful-lstm]. All you have to do is just to upload the model to the server. 

> Note: At this stage you must start your docker image with `docker-compose up` in `hydro-serving/integrations` folder if you haven't done that yet.

```sh
$ hs upload
```

Additionally Stateful LSTM needs [pre][stateful-lstm-pre] and [post][stateful-lstm-post] processing stages. So we upload them as well.

Your models now have been uploaded to ML Lambda. You can find them here - [http://127.0.0.1/models][models]. Let's go and create an application that can use your models. 

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
[models]: http://127.0.0.1/models
[applications]: http://127.0.0.1/applications
