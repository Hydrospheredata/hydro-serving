# Kubernetes installation

There's already a pre-built [Helm](https://helm.sh/) charts for installing 
and maintaining Hydrosphere on [Kubernetes](https://kubernetes.io/) cluster.

## Prerequisites

- [Helm 2.9+](https://docs.helm.sh/using_helm/#install-helm)
- [Kubernetes 1.8+ with Beta APIs enabled](https://kubernetes.io/docs/setup/)
- PV support on underlying infrastructure (if persistence is required)
- Docker Registry with pull/push access (if the built-in one is not used)


## Installation 

The following installation options are available. 

### Helm chart from repository 

Add Serving to the repo.

```sh
helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving/helm 
```

Install the chart from repo to the cluster.

```sh
helm install --name serving hydrosphere/serving
```

### Helm chart from releases

Choose a release from the 
[releases page](https://github.com/Hydrospheredata/hydro-serving/releases) 
and install it as usual.
   
```sh
helm install --name serving https://github.com/Hydrospheredata/hydro-serving/releases/download/2.0.4/helm.serving-2.0.4.tgz
```

### Helm with manual build

Clone the repository.

```sh
git clone https://github.com/Hydrospheredata/hydro-serving.git
cd hydro-serving/helm
```

Build dependencies.

```sh
helm dependency build serving
```

Install the chart.

```sh
helm install --name serving
```

After the chart was installed you have to expose an `ui` component outside 
of the cluster. For the simplicity we will just port-forward it locally. 

```sh
kubectl port-forward deployment/serving-ui 80:80
```

To check that everything works fine, open [http://localhost/](http://localhost/).

@@@ note
For more information about configuring serving release visit [this repository](https://github.com/Hydrospheredata/hydro-serving/tree/master/helm).
@@@