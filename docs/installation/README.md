# Installation

The Hydrosphere platform can be installed in the following orchestrator's:

1. [Docker Compose](./#docker-installation)
2. [Kubernetes](./#kubernetes-installation)

## Docker installation

To install Hydrosphere using `docker-compose`, you should have the following prerequisites installed on your machine.

* [Docker 18.0+](https://docs.docker.com/install/)
* [Docker Compose 1.23+](https://docs.docker.com/compose/install/#install-compose)

### Install from releases

1. Download the latest $released\_version$ release from the

   [releases page](https://github.com/Hydrospheredata/hydro-serving/releases);

```bash
   $ export HYDROSPHERE_RELEASE=$released_version$
   $ wget -O hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz https://github.com/Hydrospheredata/hydro-serving/archive/${HYDROSPHERE_RELEASE}.tar.gz
```

1. Unpack the tar ball:

```bash
   $ tar -xvf hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz
```

1. Set up an environment.

```bash
   $ cd hydro-serving-${HYDROSPHERE_RELEASE}
   $ docker-compose up
```

### Install from source

1. Clone the serving repository.

   ```bash
   $ git clone https://github.com/Hydrospheredata/hydro-serving
   ```

2. Set up an environment.

   ```bash
   $ cd hydro-serving
   $ docker-compose up -d
   ```

To check the installation, open [http://localhost/](http://localhost/). By default, Hydrosphere UI is available at port **80**.

## Kubernetes installation

To install Hydrosphere on the Kubernetes cluster you should have the following prerequisites fulfilled.

* [Helm 2.9+;](https://docs.helm.sh/using_helm/#install-helm)
* [Kubernetes 1.8+ with beta APIs enabled;](https://kubernetes.io/docs/setup/)
* PV support on the underlying infrastructure \(if persistence is

  required\);

* Docker registry with pull/push access \(if the built-in one is not

  used\).

### Install from charts repository

1. Add the Hydrosphere charts repository;

   ```bash
   $ helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving/helm
   ```

2. Install the chart from repo to the cluster.

   ```bash
   $ helm install --name serving --namespace hydrosphere hydrosphere/serving
   ```

### Install from source

1. Clone the repository.

   ```bash
   $ git clone https://github.com/Hydrospheredata/hydro-serving.git
   $ cd hydro-serving/helm
   ```

2. Build dependencies.

   ```bash
   $ helm dependency build serving
   ```

3. Install the chart.

   ```bash
   $ helm install --namespace hydrosphere serving
   ```

After the chart has been installed, you have to expose the `ui` component outside of the cluster. For the sake of simplicity, we will just port-forward it locally.

```bash
kubectl port-forward -n hydrosphere svc/hydro-serving-ui-serving 8080:9090
```

To check the installation, open [http://localhost:8080/](http://localhost:8080/).

