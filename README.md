<p align="center">
  <a href="https://hydrosphere.io/">
    <img src="https://hydrosphere.io/serving-docs/dev/images/navbar_brand.svg" alt="Hydrosphere.io logo" width="128" height="128">
  </a>
</p>

<h3 align="center">Hydrosphere Serving</h3>

<p align="center">
Platform for deploying your Machine Learning to production
  <br>
  <a href="https://hydrosphere.io/serving-docs/latest/index.html"><strong>Check out Hydrosphere.io docs »</strong></a>
  <br>
  <br>
  <a href="https://github.com/Hydrospheredata/hydro-serving/issues/new">Report bug</a>
  ·
  <a href="https://hydrosphere.io/contact/">Contact Us</a>
  ·
  <a href="https://hydrosphere.io/blog/">Blog</a>
</p>

---

[![GitHub license](https://img.shields.io/badge/license-apache-blue.svg)](https://github.com/Hydrospheredata/hydro-serving/blob/update-readme/LICENSE)
[![Join the chat at https://gitter.im/Hydrospheredata/hydro-serving](https://badges.gitter.im/Hydrospheredata/hydro-serving.svg)](https://gitter.im/Hydrospheredata/hydro-serving?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![](https://img.shields.io/badge/documentation-latest-af1a97.svg)](https://hydrosphere.io/serving-docs/) 

Hydrosphere Serving is a cluster for deploying and versioning  your machine learning models in production.

- **Framework Agnostic**. Serve machine learning models developed in any language or framework. Hydrosphere Serving will wrap them in a Docker image and deploy on your production cluster, exposing HTTP, GRPC and Kafka interfaces.
- **Traffic split**. Split your production traffic between your models to perform an A\B test or canary deployment. 
- **Traffic shadowing**. Shadow your traffic between different model versions to examine how different model versions behave on the same traffic.
- **Model Version Control**. Version control your models and pipelines as they are deployed. Explore how metrics change, roll-back to a previous version and more.


## Getting Started

You can refer to our [documentation](https://hydrosphere.io/serving-docs/latest/index.html) to see tutorials, check out [example projects]([https://github.com/Hydrospheredata/hydro-serving-example](https://github.com/Hydrospheredata/hydro-serving-example)), and learn about all features of Hydrosphere.

## Installation

There are two main ways of installing Hydropshere:
* [Docker](https://hydrosphere.io/serving-docs/latest/install/docker.html);
* [Kubernetes](https://hydrosphere.io/serving-docs/latest/install/kubernetes.html).


### Docker

Before installing Hydrosphere Serving, please install its prerequisites: 

* [Docker 18.0+](https://docs.docker.com/install/);
* [Docker Compose 1.23+](https://docs.docker.com/compose/install/#install-compose).

To install Hydrosphere Serving, follow the instructions below:

1. Download the latest release version from the [releases](https://github.com/Hydrospheredata/hydro-serving/releases) page;
    ```sh 
    export HYDROSPHERE_RELEASE=2.3.1
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

To check installation, open http://localhost/. By default Hydrosphere UI is available at port 80.

**Note**, other installation options are described in the [documentation](https://hydrosphere.io/serving-docs/latest/index.html).

### Kubernetes

Before installing Hydrosphere Serving, please install its prerequisites: 

* [Helm 2 starting from 2.9+](https://docs.helm.sh/using_helm/#install-helm) with the tiller installed on the cluster;
* [Kubernetes 1.8 and above](https://kubernetes.io/docs/setup/) with beta APIs enabled.

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
Keep up to date and get Hydrosphere.io support via [![Join the chat at https://gitter.im/Hydrospheredata/hydro-serving](https://badges.gitter.im/Hydrospheredata/hydro-serving.svg)](https://gitter.im/Hydrospheredata/hydro-serving?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) or contact us directly at [info@hydrosphere.io](mailto:info@hydrosphere.io)

### Contributing

We'd be glad to receive any help from the community!

Check out our issues for anything labeled with `help-wanted`, they will be the perfect starting point! If you don't see any, just let us know, we would be happy to hear from you.
