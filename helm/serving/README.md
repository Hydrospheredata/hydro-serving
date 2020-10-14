
Serving
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Serving chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.shadowing_on` | Enable shadowing support | `true` |
| `global.frontend_url` | Frontend url | `"http://hydro-serving.local"` |
| `global.alerting_on` | Enable alert events by prometheus alert manager | `false` |
| `global.persistence.enabled` | Enable persistence s3 bucket | `false` |
| `global.dockerRegistry.enabled` | Enable internal docker registry. manager use it for store model image | `true` |
| `global.dockerRegistry.host` | Dns name of docker registry | `""` |
| `global.dockerRegistry.username` | Docker registry username | `"example"` |
| `global.dockerRegistry.password` | Docker registry password | `"example"` |
| `global.hydrosphere.docker.host` | Dns name of docker registry where store all service image (like a manager, ui, gateway etc) | `""` |
| `global.hydrosphere.docker.username` | Docker registry username | `""` |
| `global.hydrosphere.docker.password` | Docker registry password | `""` |
| `global.mongodb.usePassword` | Use mongo with password auth | `true` |
| `global.mongodb.mongodbRootPassword` | Superuser password | `"hydr0s3rving"` |
| `global.mongodb.mongodbUsername` | Mongodb username | `"root"` |
| `global.mongodb.mongodbPassword` | Mongodb password | `"hydr0s3rving"` |
| `global.mongodb.mongodbAuthDatabase` | Mongodb authdatabase | `"admin"` |
| `global.mongodb.mongodbDatabase` | Mongodb database | `"hydro-serving-data-profiler"` |
| `mongodb.enabled` | Enable mongodb usage | `true` |
| `mongodb.image.repository` | Docker repository to pull the image from | `"bitnami/mongodb"` |
| `mongodb.image.tag` | Image tag to use | `"4.0.13-debian-9-r22"` |
| `mongodb.image.pullPolicy` | Policy for kubernetes to use when pulling images | `"IfNotPresent"` |
| `postgresql.enabled` |  | `true` |
| `postgresql.image.repository` |  | `"bitnami/postgresql"` |
| `postgresql.image.tag` |  | `9.6` |
| `postgresql.image.pullPolicy` |  | `"IfNotPresent"` |
| `postgresql.postgresqlUsername` |  | `"postgres"` |
| `postgresql.postgresqlPassword` |  | `"hydr0s3rving"` |
| `postgresql.postgresqlDatabase` |  | `"hydro-serving"` |
| `ui.public` |  | `true` |
| `ui.ingress.enabled` |  | `false` |
| `ui.ingress.annotations` |  | `{}` |
| `ui.ingress.hosts` |  | `["hydro-serving.local"]` |
| `ui.ingress.path` |  | `"/"` |
| `ui.ingress.tls` |  | `[]` |
| `manager.postgres.postgresqlUsername` |  | `"postgres"` |
| `manager.postgres.postgresqlPassword` |  | `"hydr0s3rving"` |
| `manager.postgres.postgresqlDatabase` |  | `"hydro-serving"` |
| `docker-registry.proxy.enabled` |  | `true` |
| `docker-registry.ingress.enabled` |  | `false` |





