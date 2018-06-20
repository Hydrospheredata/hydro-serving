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

Next, clone ML Lambda (_hydro-serving_) into desired directory.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```

Now set up a docker environment. This will take a while. 

```sh
$ cd ./hydro-serving/integrations/
$ docker-compose up # --no-start if you don't want to launch an image instanly
```

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

> Note, at this stage you must start your docker image with `docker-compose up` in `hydro-serving/integrations` folder.

```sh
$ hs upload
```

Additionally Stateful LSTM needs [pre][stateful-lstm-pre] and [post][stateful-lstm-post] processing stages. So we upload them as well.

Now your models are uploaded to the server. You can find them here - [http://127.0.0.1/models][models]. Let's go and create an application that can use your models. But before we can do that, we need to install _runtimes_. 

_Runtime_ is an environment with the pre-installed dependencies. Those can be [Python][docker-hub-python], [Spark][docker-hub-spark], [Tensorflow][docker-hub-tensorflow] and so on. All of them are stored in our [Docker Hub][docker-hub] and have _runtime_ in the name of the directory. By default you have only pre-installed `hydrosphere/serving-runtime-dummy:latest` runtime. To install additional ones execute:

```sh
curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
   "name": "hydrosphere/serving-runtime-python",
   "version": "3.6-latest",
   "modelTypes": [
     "string"
   ],
   "tags": [
     "string"
   ],
   "configParams": {}
 }' 'http://localhost:8080/api/v1/runtime'
```

This particular command will add Python 3.6 to you runtimes. 

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
