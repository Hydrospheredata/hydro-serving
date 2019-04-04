# Docker installation

## Prerequisites

- [Docker 18.0+][docker-install]
- [Docker Compose 1.23+][docker-compose-install]


## Install from release

Download and unpack the latest released version from [releases page](https://github.com/Hydrospheredata/hydro-serving/releases). 


## Install from source

Clone serving repository.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```


Once you've obtained Serving files, you can deploy one of the 2 versions: 

1. The __lightweight__ version lets you manage your models in a continuous manner, version them, create applications that use your models and deploy all of this into production. To do that, just execute the following: 

    ```sh
    $ cd hydro-serving/
    $ docker-compose up -d 
    ```

1. The __integrations__ version extends the __lightweight__ version and lets you also integrate kafka, grafana and influxdb.

    ```sh
    $ cd hydro-serving/integrations/
    $ docker-compose up -d
    ```

To check that everything works fine, open [http://localhost/](http://localhost/). By default UI is available at port __80__.


[docker-install]: https://docs.docker.com/install/
[docker-compose-install]: https://docs.docker.com/compose/install/#install-compose
