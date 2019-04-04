# Kubernetes installation

There's already a pre-built [Helm](https://helm.sh/) charts for installing and maintaining Serving on [Kubernetes](https://kubernetes.io/) clusters.

## Prerequisites

- [Helm 2.9+](https://docs.helm.sh/using_helm/#install-helm)
- [Kubernetes 1.8+ with Beta APIs enabled](https://kubernetes.io/docs/setup/)
- PV support on underlying infrastructure (if persistence is required)
- Docker Registry with pull/push access (if the built-in one is not used)


Installation can be performed in a few ways:

## Install helm chart from repository

Add Serving to the repo.

```sh
$ helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving/helm 
```

Install the chart from repo to the cluster.

```sh
$ helm install --name serving hydrosphere/serving
```

## Install helm chart from release

Choose a release from the [releases page](https://github.com/Hydrospheredata/hydro-serving/releases) and install it as usual.
   
```sh
$ helm install --name serving https://github.com/Hydrospheredata/hydro-serving/releases/download/2.0.0/helm.serving-2.0.0.tgz
```

## Install helm chart manually from source

Clone the repository.

```sh
$ git clone https://github.com/Hydrospheredata/hydro-serving.git
$ cd hydro-serving/helm
```

Build dependencies.

```sh
$ helm dependency build serving
```

Install the chart.

```sh
$ helm install --name serving
```

After chart was installed to the cluster, you have to expose an `ui` deployment outside of the cluster in order to access it. For the simplicity, I will just port-forward sidecar locally. 

```sh
$ kubectl port-forward deployment/serving-ui 80:80
```

To check that everything works fine, open [http://localhost/](http://localhost/). By default UI is available at port __80__.

For more information about configuring serving release refer to the [chart's repository](https://github.com/Hydrospheredata/hydro-serving-helm/tree/master/).
