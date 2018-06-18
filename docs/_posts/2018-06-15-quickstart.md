---
layout: post
title:  "Quickstart"
date:   2018-06-15 13:57:31 +0300
permalink: 'quickstart.html'
---

This document present a quick way to set everything up and deploy your first
model on various environments.

* [Localhost](#localhost)
* AWS
* Google Cloud Engine


# Localhost
{: #localhost}

To get started, download [Docker][docker-install] on your machine if you don't
have one. 

Next, clone ML Lambda (_hydro-serving_) into desired directory.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```

Now you have 2 options. 
1. Set up full environment, which contains [Sonar][hydro-sonar] and supports 
[Appache Kafka][appache-kafka]. You can do this by composing containers from
`integrations` directory.

	```sh
	$ cd ./hydro-serving/integrations/
	$ docker-compose up
	```

2. Compose a lightweight environment without supporting full integrations. 

	```sh 
	$ cd ./hydro-serving
	$ docker-compose up
	```

This might take a while, but after that you can go to http://127.0.0.1/ and open
web interface. 

Now, that you're ready with the docker, let's go ahead and deploy your
first model. For this purpose we've built a handful [cli-tool]
[hydro-serving-cli].

```sh
$ pip install hs
```

If you don't have any ready models at the moment, you can use our [examples]
[hydro-serving-examples] and work with them. 

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-example
$ cd ./hydro-serving-examples/models/$MODEL_OF_YOUR_CHOICE
```

All you have to do is just to upload the model to the server. Note, that `hs
upload` has optional parameters with default values `--host localhost --port
9090`.

```sh
$ hs upload
```

After that your model will be uploaded to the server and 


[docker-install]: https://docs.docker.com/install/
[hydro-sonar]: https://hydrosphere.io/sonar/
[hydro-serving-cli]: https://github.com/Hydrospheredata/hydro-serving-cli
[hydro-serving-examples]: https://github.com/Hydrospheredata/hydro-serving-example
[appache-kafka]: https://kafka.apache.org
