<p align="center">
  <a href="https://hydrosphere.io/">
    <img src="https://gblobscdn.gitbook.com/spaces%2F-MESaD8WY3ggQLtBByXl%2Favatar-1597150668933.png?alt=media" alt="Hydrosphere.io logo" width="128" height="128">
  </a>
</p>

<h3 align="center">Hydrosphere Serving</h3>

<p align="center">
Platform for deploying your Machine Learning to production
  <br>
  <a href="https://docs.hydrosphere.io"><strong>Check out Hydrosphere.io docs »</strong></a>
  <br>
  <br>
  <a href="https://github.com/Hydrospheredata/hydro-serving/issues/new">Report bug</a>
  ·
  <a href="https://hydrosphere.io/contact/">Contact Us</a>
 
</p>

---

[![GitHub license](https://img.shields.io/badge/license-apache-blue.svg)](https://github.com/Hydrospheredata/hydro-serving/blob/update-readme/LICENSE)
[![](https://img.shields.io/badge/chat-on%20slack-%23E01E5A)](https://hydrospheredata.slack.com/join/shared_invite/zt-tt4j24xj-TpnI_D2aJDBHIbA~EmPSlQ#/shared-invite/email)
[![](https://img.shields.io/badge/documentation-latest-af1a97.svg)](https://docs.hydrosphere.io) 
[![Helm chart Lint and Testing](https://github.com/Hydrospheredata/hydro-serving/actions/workflows/Lint%20helm.yaml/badge.svg)](https://github.com/Hydrospheredata/hydro-serving/actions/workflows/Lint%20helm.yaml)

Hydrosphere Serving is a cluster for deploying and versioning  your machine learning models in production.

- **Framework Agnostic**. Serve machine learning models developed in any language or framework. Hydrosphere Serving will wrap them in a Docker image and deploy on your production cluster, exposing HTTP, gRPC and Kafka interfaces.
- **Traffic shadowing**. Shadow your traffic between different model versions to examine how different model versions behave on the same traffic.
- **Model Version Control**. Version control your models and pipelines as they are deployed. 


## Getting Started

You can refer to our [documentation](https://hydrosphere.io/serving-docs/latest/index.html) to see tutorials, check out [example projects]([https://github.com/Hydrospheredata/hydro-serving-example](https://github.com/Hydrospheredata/hydro-serving-example)), and learn about all features of Hydrosphere.

## Installation

There are two main ways of installing Hydropshere:
* [Docker](https://docs.hydrosphere.io/quickstart/installation#docker-installation);
* [Kubernetes](https://docs.hydrosphere.io/quickstart/installation#kubernetes-installation).


### Docker

Before installing Hydrosphere Serving, please install its prerequisites: 

* [Docker 20+, with BuildKit enabled](https://docs.docker.com/install/);
* [Docker Compose 1.29+](https://docs.docker.com/compose/install/#install-compose).

To deploy the Hydrosphere platform from master branch, follow the instructions below:

1. Download the latest release version from the [releases](https://github.com/Hydrospheredata/hydro-serving/releases) page;
    ```sh 
    ### __released_version__
    export HYDROSPHERE_RELEASE=3.0.3

    wget -O hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz https://github.com/Hydrospheredata/hydro-serving/archive/${HYDROSPHERE_RELEASE}.tar.gz
    ```
1. Unpack the tar ball;
    ```sh 
    tar -xvf hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz
    ```
1. Set up an environment.
    ```sh
    cd hydro-serving-${HYDROSPHERE_RELEASE}
    docker-compose up
    ```

To deploy the Hydrosphere platform from the master branch, follow the instructions below:

1. Clone umbrella repository locally.
    ```sh
    git clone ...
    cd hydro-serving
    ```
1. Build the docker-compose.yaml.
    ```sh
    ./build-compose.sh
    docker-compose up
    ```
1. Set up an environment.
    ```sh
    docker-compose up
    ```

To check installation, open http://localhost/. By default Hydrosphere UI is available at port 80.

**Note**, other installation options are described in the [documentation](https://hydrosphere.io/serving-docs/latest/index.html).

### Kubernetes

Before installing Hydrosphere Serving, please install its prerequisites: 

* [Helm 3.0+](https://docs.helm.sh/using_helm/#install-helm) with the tiller installed on the cluster;
* [Kubernetes 1.16+ with v1 API](https://kubernetes.io/docs/setup/) with beta APIs enabled.

To install Hydrosphere Serving, follow the instructions below:

```shell
helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving/helm/
helm install --name serving --namespace hydrosphere hydrosphere/serving
```

To reach the cluster, port-forward `ui` service locally. 

```
kubectl port-forward -n hydrosphere svc/serving-ui 8080:9090
```

To check installation, open http://localhost:8080/.

**Note**, other installation options are described in the [documentation](https://hydrosphere.io/serving-docs/latest/index.html). 

## Community
Keep up to date and get Hydrosphere.io support via [![](https://img.shields.io/badge/chat-on%20slack-%23E01E5A)](https://hydrospheredata.slack.com/join/shared_invite/zt-tt4j24xj-TpnI_D2aJDBHIbA~EmPSlQ#/shared-invite/email) or contact us directly at [info@hydrosphere.io](mailto:info@hydrosphere.io)

### Contributing

We'd be glad to receive any help from the community!

Check out our issues for anything labeled with `help-wanted`, they will be the perfect starting point! If you don't see any, just let us know, we would be happy to hear from you.
