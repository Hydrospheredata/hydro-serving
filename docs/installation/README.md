# Installation

The Hydrosphere platform can be installed in the following orchestrator's:

1. [Docker Compose](./#docker-installation)
2. [Kubernetes](./#kubernetes-installation)

## Docker installation

To install Hydrosphere using `docker-compose`, you should have the following prerequisites installed on your machine.

* [Docker 18.0+](https://docs.docker.com/install/)
* [Docker Compose 1.23+](https://docs.docker.com/compose/install/#install-compose)

### Install from releases

1. Download the latest $released\_version$ release from the [releases page](https://github.com/Hydrospheredata/hydro-serving/releases):

```bash
export HYDROSPHERE_RELEASE=$released_version$
wget -O hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz https://github.com/Hydrospheredata/hydro-serving/archive/${HYDROSPHERE_RELEASE}.tar.gz
```

1. Unpack the tar ball:

```bash
tar -xvf hydro-serving-${HYDROSPHERE_RELEASE}.tar.gz
```

1. Set up an environment:

```bash
cd hydro-serving-${HYDROSPHERE_RELEASE}
docker-compose up
```

### Install from source

1. Clone the serving repository:

   ```bash
   git clone https://github.com/Hydrospheredata/hydro-serving
   ```

2. Set up an environment:

   ```bash
   cd hydro-serving
   docker-compose up -d
   ```

To check the installation, open [http://localhost/](http://localhost/). By default, Hydrosphere UI is available at port **80**.

## Kubernetes installation

To install Hydrosphere on the Kubernetes cluster you should have the following prerequisites fulfilled.

* [Helm 2.9+](https://docs.helm.sh/using_helm/#install-helm)
* [Kubernetes 1.14+ with v1 API](https://kubernetes.io/docs/setup/)
* PV support on the underlying infrastructure \(if persistence is required\)
* Docker registry with pull/push access \(if the built-in one is not used\)

### Install from charts repository

1. Add the Hydrosphere charts repository:

   ```bash
   helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving/helm
   ```

2. Install the chart from repo to the cluster:

   ```bash
   helm install --name serving --namespace hydrosphere hydrosphere/serving
   ```

### Install from source

1. Clone the repository:

   ```bash
   git clone https://github.com/Hydrospheredata/hydro-serving.git
   cd hydro-serving/helm
   ```

2. Build dependencies:

   ```bash
   helm dependency build serving
   ```

3. Install the chart:

   ```bash
   helm install --namespace hydrosphere serving
   ```

After the chart has been installed, you have to expose the `ui` component outside of the cluster. For the sake of simplicity, we will just port-forward it locally.

```bash
kubectl port-forward -n hydrosphere svc/hydro-serving-ui-serving 8080:9090
```

To check the installation, open [http://localhost:8080/](http://localhost:8080/).

# Local developing

We use Docker client's option to start Kubernetes internally (it works as Minikube)

#### Prerequisites
* Helm (checked for version v3.5.0 on macOS BigSur)
* Docker client ( checked for version 20.10.5 on macOS BigSur)

#### Strating project
1. Turn on kubernetes in docker client [more info](https://docs.docker.com/desktop/kubernetes/#:~:text=To%20enable%20Kubernetes%20support%20and,Stacks%20to%20Kubernetes%20by%20default).
2. Clone hydro-serving repository ```https://github.com/Hydrospheredata/hydro-serving```
3. ```cd hydro-serving/helm/serving```
4. Using docker registry, there are two options:
   1. **Using local docker registry**
      1. Set up registry ```docker run -d -p 5000:5000 --name registry registry:2```
      2. Add insecure registry to docker config
         ```"insecure-registries": ["<your_local_ip_address>:5000"]```
      3. Edit registry field inside ```hydro-serving/helm/serving/values.yaml```
            ``` registry:
              insecure: true
              ingress:
                enabled: false
                host: "hydrosphere-registry.local"
                path: "/"
                issuer: "letsencrypt-prod"
              url: "<your_local_ip_address>:5000"
              username: ""
              password: ""
   2. **Using docker hub**
      1. edit registry field inside ```hydro-serving/helm/serving/values.yaml```
           ``` registry:
               insecure: false
               ingress:
                 enabled: false
                 host: "hydrosphere-registry.local"
                 path: "/"
                 issuer: "letsencrypt-prod"
               url: "docker.io/<docker_login>"
               username: "<docker_login>"
               password: "<docker_password>"
3.  ```helm dependency update```
4.  ```helm upgrade --install hydrosphere .```
5.  Use command ```kubectl get po``` to check that all pods are ok
6.  Port forward http -  ```kubectl port-forward svc/hydrosphere-ui 80:9090```
7.  Port forward grpc -  ```kubectl port-forward svc/hydrosphere-ui 9090:9091```
8.  Open browser, by default ```localhost:80```, you should see a Hydrosphere's UI

### Working with local images
1. Locally build new docker image of some service
2. Find deployment that you need ```kubectl get deploy``` , i.e ```hydrosphere-ui```
3. Edit that deployment deployment ```kubectl edit deploy hydrosphere-ui```
4. Edit template, which will use your new image. [How to edit](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#updating-a-deployment)
5. After save, Kubernetes will restart your service, it takes some time


