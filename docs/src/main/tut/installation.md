---
layout: docs
title:  "Installation"
permalink: 'installation.html'
---

# Installation 

You can setup ML Lambda instance in 2 differents ways: 

1. [Setup on Docker Environment]({{site.baseurl}}{%link installation.md%}#docker)
1. [Setup on Kubernetes Cluster]({{site.baseurl}}{%link installation.md%}#kubernetes)

ML Lambda works along with the [cli-tool]({{site.baseurl}}{%link installation.md%}#cli). Make sure, you've installed it too. 

<br>
<br>

## Docker

### Prerequisites

- [Docker 18.0+][docker-install]
- [Docker Compose 1.23+][docker-compose-install]

Docker installation doesn't differ much except that from where you're taking your source files. 

<hr>

#### From Releases

Download and unpack the latest released version from [releases page](https://github.com/Hydrospheredata/hydro-serving). 

<hr>

#### From Source 

Clone serving repository.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving
```

<hr>

Once you've obtained ML Lambda files, you can deploy one of the 2 versions: 

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

<br>
<br>

## Kubernetes

There's already a pre-built [Helm](https://helm.sh/) charts for installing and maintaining ML Lambda on [Kubernetes](https://kubernetes.io/) clusters.

### Prerequisites

- [Helm 2.9+](https://docs.helm.sh/using_helm/#install-helm)
- [Kubernetes 1.8+ with Beta APIs enabled](https://kubernetes.io/docs/setup/)
- PV support on underlying infrastructure (if persistence is required)
- Docker Registry with pull/push access (if the built-in one is not used)


Installation can be performed in a few ways. 

<hr>

#### From Repo

Add ML Lambda to the repo.

```sh
$ helm repo add ml-lambda https://hydrospheredata.github.io/hydro-serving-helm/
```

Install the chart from repo to the cluster.

```sh
$ helm install --name ml-lambda ml-lambda/serving
```

<hr>

#### From Release

Choose a release from the [releases page](https://github.com/Hydrospheredata/hydro-serving-helm/releases) and install it as usuall.
   
```sh
$ helm install --name ml-lambda https://github.com/Hydrospheredata/hydro-serving-helm/releases/download/0.1.15/serving-0.1.15.tgz
```

<hr>

#### From Source

Clone the repository.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving-helm.git
$ cd hydro-serving-helm
```

Build dependencies.

```sh
$ helm dependency build serving
```

Install the chart.

```sh
$ helm install --name ml-lambda serving
```

<hr>

To check that everything works fine, open [http://localhost/](http://localhost/). By default UI is available at port __80__.

For more information about configuring ml-lambda release refer to the [chart's repository](https://github.com/Hydrospheredata/hydro-serving-helm).

<br>
<br>

## CLI

### Prerequisites

- [Python 3.6+](https://www.python.org/downloads/)

To install cli-tool, run:

```sh 
$ pip install hs
```

<hr>

# What's Next? 

- [Learn, how to get started with ML Lambda]({{site.baseurl}}{%link tutorials/getting-started.md%});

[docker-install]: https://docs.docker.com/install/
[docker-compose-install]: https://docs.docker.com/compose/install/#install-compose