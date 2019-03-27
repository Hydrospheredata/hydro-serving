# Hydro Serving Helm

A collection of [Helm](https://helm.sh) charts to install and maintain
a core [HydroServing](https://github.com/Hydrospheredata/hydro-serving) on [Kubernetes](https://kubernetes.io).

Includes charts for HydroServing:

- [Manager](https://github.com/Hydrospheredata/hydro-serving)
- [Sidecar](https://github.com/Hydrospheredata/hydro-serving-sidecar)
- [Gateway](https://github.com/Hydrospheredata/hydro-serving-gateway)
- [UI](https://github.com/Hydrospheredata/hydro-serving-ui)

And infrastructure components (optional):

- Postgres
- InfluxDB
- ElasticSearch
- Docker Registry

The charts themselves will not install and run directly. They are
included in the `serving` chart as requirements.

## Prerequisites Details

- Kubernetes 1.8+ with Beta APIs enabled
- PV support on underlying infrastructure (if persistence is required)
- Docker Registry with pull/push access (if the built-in one is not used)

## [WARNING] Docker Registry

This chart contains private docker registry enabled by default. It
should be used only for the purpose of "try-and-remove" as its configuration is
a little bit hacky (ok, it is _very_ hacky): registry chart opens
`hostPort` on **every** kubernetes node ([why this is a bad idea](https://kubernetes.io/docs/concepts/configuration/overview/#services)). Please, do not use built-in registry in real life.


## Installing the Chart

### From Repo

Add hydro-serving repo

```
$ helm repo add hydro-serving https://hydrospheredata.github.io/hydro-serving-helm/
```

Install the chart:

```
$ helm install --name my-release hydro-serving/serving
```

### From GitHub Releases

Choose a release on the [Releases Page](https://github.com/Hydrospheredata/hydro-serving-helm/releases)
and install it as usual:

```
$ helm install --name my-release https://github.com/Hydrospheredata/hydro-serving-helm/releases/download/0.1.17/serving-0.1.17.tgz
```

### From source

Foremost, build the dependencies:

```
$ helm dependency build serving
```

To install the chart with the release name `my-release`:


```
$ helm install --name my-release serving
```

The command deploys HydroServing on the Kubernetes cluster in the default configuration. The [configuration](#configuration) section lists the parameters that can be configured during installation.

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```
$ helm delete my-release
```
The command removes all the Kubernetes components associated with the chart and deletes the release.

## Configuration

The following table lists the configurable parameters of the HydroServing chart and their default values.

| Parameter                             | Description                                           | Default                           |
|---------------------------------------|-------------------------------------------------------|-----------------------------------|
| `global.persistence.enabled`          | Use persistence for registry and dbs                  | `false`                           |
| `global.persistence.accessMode`       | Access mode to use for PVC                            | `ReadWriteOnce`                   |
| `global.persistence.size`             | Amount of space to claim for PVC                      | `10Gi`                            |
| `global.persistence.storageClass`     | Storage Class to use for PVC                          | `-`                               |
| `global.persistence.existingClaim`    | Name of an existing PVC to use for config             | `nil`                             |
| `global.dockerRegistry.enabled`       | Whether to install built-in docker registry           | `true`                            |
| `global.dockerRegistry.host`          | External docker registry host                         | `""`                              |
| `global.dockerRegistry.username`      | Docker registry username                              | `user`                            |
| `global.dockerRegistry.password`      | Docker registry password                              | `pass`                            |
| `global.postgres.enabled`             | Whether to install built-in Postgres                  | `true`                            |
| `global.postgres.host`                | External postgres host (if enabled = false)           | `nil`                             |
| `global.postgres.port`                | Postgres port                                         | `nil`                             |
| `global.postgres.database`            | Postgres database name                                | `nil`                             |
| `global.postgres.username`            | Postgres username                                     | `nil`                             |
| `global.postgres.password`            | Postgres password                                     | `nil`                             |
| `global.elasticsearch.enabled`        | Whether to install built-in ElasticSearch             | `true`                            |
| `global.elasticsearch.host`           | External elasticsearch host (if enabled = false)      | `nil`                             |
| `global.influx.enabled`               | Whether to install built-in InfluxDB                  | `true`                            |
| `global.influx.host`                  | External InfluxDB host (if enabled = false)           | `nil`                             |
| `global.influx.port`                  | InfludDB port                                         | `nil`                             |
| `global.influx.database`              | InfluxDB database naem                                | `nil`                             |
| `manager.image.repository`            | Container image for `manager`                         | `hydrosphere/serving-manager`     |
| `manager.image.tag`                   | Container image tag for `manager`                     | `latest`                          |
| `manager.image.pullPolicy`            | Container pull policy                                 | `Always`                          |
| `manager.serviceAccount.create`       | Specifies whether a ServiceAccount should be created  | `true`                            |
| `manager.serviceAccount.name`         | The name of the ServiceAccounts to use.               | `nil` (auto-generated)            |
| `sidecar.image.repository`            | Container image for `sidecar`                         | `hydrosphere/serving-sidecar`     |
| `sidecar.image.tag`                   | Container image tag for `sidecar`                     | `latest`                          |
| `sidecar.image.pullPolicy`            | Container pull policy                                 | `Always`                          |
| `sidecar.ingress.enabled`             | Whether to install ingress to expose hydro-serving    | `false`                            |
| `sidecar.ingress.annotations`         | Kubernetes annotations for ingress                    | `{}`                              |
| `sidecar.ingress.path`                | Ingress path                                          | `/`                               |
| `sidecar.ingress.hosts`               | Ingress hosts                                         | `- hydro-serving.local`           |
| `sidecar.ingress.tls`                 | TLS configuration                                     | `[]`                              |
| `gateway.image.repository`            | Container image for `gateway`                         | `hydrosphere/serving-gateway`     |
| `gateway.image.tag`                   | Container image tag for `gateway`                     | `latest`                          |
| `gateway.image.pullPolicy`            | Container pull policy                                 | `Always`                          |
| `ui.image.repository`                 | Container image for `ui`                              | `hydrosphere/serving-manager-ui`  |
| `ui.image.tag`                        | Container image tag for `ui`                          | `latest`                          |
| `ui.image.pullPolicy`                 | Container pull policy                                 | `Always`                          |

Specify each parameter using the --set key=value[,key=value] argument to helm install. For example,

```
$ helm install --name my-release \
    --set global.persistence.enabled=true \
    serving
```

Alternatively, a YAML file that specifies the values for the above parameters can be provided while installing the chart. For example,

```
$ helm install --name my-release -f values.yaml serving
```

## Ingress and TLS

To get Helm to create an ingress object with a hostname, add these two lines to your Helm command:

```
$ helm install --name my-release \
  --set sidecar.ingress.enabled=true \
  --set sidecar.ingress.hosts[0]="hydro-serving.company.com" \
  serving
```

If your cluster allows automatic creation/retrieval of TLS certificates (e.g. [cert-manager](https://github.com/jetstack/cert-manager)), please refer to the documentation for that mechanism.

To manually configure TLS, first create/retrieve a key & certificate pair for the address(es) you wish to protect. Then create a TLS secret in the namespace:
```
$ kubectl create secret tls hydro-serving-tls --cert=path/to/tls.cert --key=path/to/tls.key
```

Include the secret's name, along with the desired hostnames, in the Sidecar Ingress TLS section of your custom values.yaml file:

```
sidecar:
  ingress:
    enabled: true
    hosts:
      - hydro-serving.company.com
    annotations:
      kubernetes.io/tls-acme: "true"
    tls:
      - secretName: hydro-serving-tls
        hosts:
          - hydro-serving.domain.com
```

