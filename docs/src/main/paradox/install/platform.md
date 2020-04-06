# Platform

The Hydrosphere platform can be installed in the following orchestrators:

@@ toc { depth=1 }

## Docker installation

To install Hydrosphere using `docker-compose`, you should have the following prerequisites installed on your machine. 

- [Docker 18.0+](https://docs.docker.com/install/)
- [Docker Compose 1.23+](https://docs.docker.com/compose/install/#install-compose)

#### Install from releases

1. Download the latest @var[project.released_version] release from the [releases page](https://github.com/Hydrospheredata/hydro-serving/releases);

    @@@vars
    ```sh
    $ export HYDROSPHERE_RELEASE=$project.released_version$
    $ wget -O hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz https://github.com/Hydrospheredata/hydro-serving/archive/${HYDROSPHERE_RELEASE}.tar.gz
    ```
    @@@

1. Unpack the tar ball:

    @@@vars
    ```sh 
    $ tar -xvf hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz
    ```
    @@@

1. Set up an environment.

    @@@vars 
    ```sh
    $ cd hydro-serving-${HYDROSPHERE_RELEASE}
    $ docker-compose up
    ```
    @@@

#### Install from source

1. Clone the serving repository.

    ```sh
    $ git clone https://github.com/Hydrospheredata/hydro-serving
    ```

1. Set up an environment.

    ```sh
    $ cd hydro-serving
    $ docker-compose up -d
    ```

To check installation, open [http://localhost/](http://localhost/). By default Hydrosphere UI is available at port __80__.

## Kubernetes installation

To install Hydrosphere on the Kubernetes cluster you should have the following prerequisites fulfilled. 

- [Helm 2.9+;](https://docs.helm.sh/using_helm/#install-helm)
- [Kubernetes 1.8+ with beta APIs enabled;](https://kubernetes.io/docs/setup/)
- PV support on the underlying infrastructure (if persistence is required);
- Docker registry with pull/push access (if the built-in one is not used).


#### Install from charts repository

1. Add the Hydrosphere charts repository;

    ```sh
    $ helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving/helm 
    ```

1. Install the chart from repo to the cluster.

    ```sh
    $ helm install --name serving --namespace hydrosphere hydrosphere/serving
    ```

#### Install from source

1. Clone the repository.

    ```sh
    $ git clone https://github.com/Hydrospheredata/hydro-serving.git
    $ cd hydro-serving/helm
    ```

1. Build dependencies.

    ```sh
    $ helm dependency build serving
    ```

1. Install the chart.

    ```sh
    $ helm install --name serving --namespace hydrosphere
    ```

After the chart has been installed, you have to expose the `ui` component outside 
of the cluster. For the sake of simplicity, we will just port-forward it locally. 

```sh
kubectl port-forward -n hydrosphere svc/hydro-serving-ui-serving 8080:9090
```

To check installation, open [http://localhost:8080/](http://localhost:8080/).


[docker-install]: 
[docker-compose-install]: 

