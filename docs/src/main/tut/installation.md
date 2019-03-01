---
layout: docs
title:  "Installation"
permalink: 'installation.html'
---

# Installation 

You can setup Serving instance in 2 differents ways: 

1. [Setup on Docker Environment]({{site.baseurl}}{%link installation.md%}#docker)
1. [Setup on Kubernetes Cluster]({{site.baseurl}}{%link installation.md%}#kubernetes)

Serving works along with the [cli-tool]({{site.baseurl}}{%link installation.md%}#cli). Make sure, you've installed it too. 

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

<br>
<br>

## Kubernetes

There's already a pre-built [Helm](https://helm.sh/) charts for installing and maintaining Serving on [Kubernetes](https://kubernetes.io/) clusters.

### Prerequisites

- [Helm 2.9+](https://docs.helm.sh/using_helm/#install-helm)
- [Kubernetes 1.8+ with Beta APIs enabled](https://kubernetes.io/docs/setup/)
- PV support on underlying infrastructure (if persistence is required)
- Docker Registry with pull/push access (if the built-in one is not used)


Installation can be performed in a few ways:

<hr>

#### From Repo

Add Serving to the repo.

```sh
$ helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving-helm/
```

Install the chart from repo to the cluster.

```sh
$ helm install --name serving hydrosphere/serving
```

<hr>

#### From Release

Choose a release from the [releases page](https://github.com/Hydrospheredata/hydro-serving-helm/releases) and install it as usual.
   
```sh
$ helm install --name serving https://github.com/Hydrospheredata/hydro-serving-helm/releases/download/0.1.15/serving-0.1.15.tgz
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
$ helm install --name serving
```

<hr>

After chart was installed to the cluster, you have to expose a `sidecar` deployment outside of the cluster in order to access it. For the simplicity, I will just port-forward sidecar locally. 

```sh
$ kubectl port-forward deployment/serving-sidecar 8080:80
```

To check that everything works fine, open [http://localhost/](http://localhost/). By default UI is available at port __80__.

For more information about configuring serving release refer to the [chart's repository](https://github.com/Hydrospheredata/hydro-serving-helm).

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

- [Learn, how to get started with Hydrosphere Serving]({{site.baseurl}}{%link tutorials/getting-started.md%});

[docker-install]: https://docs.docker.com/install/
[docker-compose-install]: https://docs.docker.com/compose/install/#install-compose