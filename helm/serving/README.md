
Serving
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Serving chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.shadowing_on` | Enable shadowing mode | `true` |
| `global.frontend_url` | Url to frontend | `"https://ui.qa.hydrosphere.io"` |
| `global.alerting_on` | Enable prometheus alertmanager (optional) | `false` |
| `global.persistence.enabled` | Enable persistence s3 bucket for docker-registry image (optional) | `false` |
| `global.dockerRegistry.enabled` | Enable internal docker registry (required) | `true` |
| `global.dockerRegistry.host` | Url to internal docker-registry (store upload models) | `"qa-registry.dev.hydrosphere.io"` |
| `global.dockerRegistry.username` | Internal docker-registry username | `"developers"` |
| `global.dockerRegistry.password` | Internal docker-registry password | `"not-secure-password"` |
| `global.hydrosphere.docker.host` | Hydrosphere docker-registry url (store hydro-serving image, required) | `"harbor.hydrosphere.io/hydro-serving"` |
| `global.hydrosphere.docker.username` | Hydrosphere docker-registry username (required) | `"developers"` |
| `global.hydrosphere.docker.password` | Hydrosphere docker-registry password (required) | `"not-secure-password"` |
| `global.s3.accessKey` | S3 bucket accesskeyid (required if persistence.enabled: true) | `"ACCESSKEYEXAMPLE"` |
| `global.s3.secretKey` | S3 bucket secretkeyid | `"SECRETKEYEXAMPLE"` |
| `global.s3.region` | S3 bucket region | `"eu-central-1"` |
| `global.s3.bucket` | S3 bucket name | `"docker-dev-hydrosphere"` |
| `global.s3.featureLakeBucketName` | S3 featurelake bucket name (required) | `"feature-lake"` |
| `global.s3.featureLakeBucketCreate` | Сreate s3 featurelake bucket | `false` |
| `global.s3.hydrovizBucketName` | S3 hydro-viz bucket name (is created automatically) | `"hydro-viz"` |
| `global.mongodb.usePassword` | Use password mode for mongodb (optional) | `true` |
| `global.mongodb.mongodbRootPassword` | Mongodb root password | `"hydr0s3rving"` |
| `global.mongodb.mongodbUsername` | Mongodb username | `"root"` |
| `global.mongodb.mongodbPassword` | Mongodb password | `"hydr0s3rving"` |
| `global.mongodb.mongodbAuthDatabase` | Mongodb auth database | `"admin"` |
| `global.mongodb.mongodbDatabase` | Mongodb data profiler database name | `"hydro-serving-data-profiler"` |
| `mongodb.enabled` | Enable mongodb (required) | `true` |
| `mongodb.image.repository` | Repository name for mongodb image | `"bitnami/mongodb"` |
| `mongodb.image.tag` | Version image for mongodb | `"4.0.13-debian-9-r22"` |
| `mongodb.image.pullPolicy` | Pull policy (if tag latest please set always) | `"IfNotPresent"` |
| `postgresql.enabled` | Enable postgresql database (required) | `true` |
| `postgresql.image.repository` | Repository name for postgresql image | `"bitnami/postgresql"` |
| `postgresql.image.tag` | Version image for postgresql | `9.6` |
| `postgresql.image.pullPolicy` | Pull policy (if tag latest please set always) | `"IfNotPresent"` |
| `postgresql.postgresqlUsername` | Postgresql username | `"postgres"` |
| `postgresql.postgresqlPassword` | Postgresql password | `"hydr0s3rving"` |
| `postgresql.postgresqlDatabase` | Postgresql database name | `"hydro-serving"` |
| `rootcause.mongo.mongodbUsername` | Mongodb username | `"root"` |
| `rootcause.mongo.mongodbPassword` | Mongodb password | `"hydr0s3rving"` |
| `rootcause.mongo.mongodbAuthDatabase` | Mongodb auth database | `"admin"` |
| `rootcause.mongo.mongodbDatabase` | Mongodb data profiler database name | `"hydro-serving-data-profiler"` |
| `ui.public` | What version we use (public or closed) | `true` |
| `ui.ingress.enabled` | Enable access with ingress controller | `true` |
| `ui.ingress.annotations.kubernetes.io/ingress.class` | Ingress controller name | `"nginx"` |
| `ui.ingress.annotations.kubernetes.io/tls-acme` | Enable cert-manager certificate | `"true"` |
| `ui.ingress.annotations.cert-manager.io/cluster-issuer` | Cert-manager issuer name | `"letsencrypt-prod"` |
| `ui.ingress.annotations.nginx.ingress.kubernetes.io/ssl-redirect` | Redirect all trafic from http to https | `"true"` |
| `ui.ingress.annotations.nginx.ingress.kubernetes.io/client-max-body-size` | Limits max body size upload | `"0"` |
| `ui.ingress.annotations.nginx.ingress.kubernetes.io/proxy-body-size` | Limits max proxy body seze | `"0"` |
| `ui.ingress.annotations.nginx.ingress.kubernetes.io/proxy-buffering` | Disable buffering then proxy | `"off"` |
| `ui.ingress.annotations.nginx.ingress.kubernetes.io/proxy-http-version` | Http version use 1.1 | `"1.1"` |
| `ui.ingress.annotations.nginx.ingress.kubernetes.io/proxy-request-buffering` | Disable buffering proxy request | `"off"` |
| `ui.ingress.hosts` | Url hydrosphere ui | `["ui.qa.hydrosphere.io"]` |
| `ui.ingress.path` | Root path | `"/"` |
| `ui.ingress.tls` |  | `[{"hosts": ["ui.qa.hydrosphere.io"], "secretName": "qa-hydro-tls"}]` |
| `manager.postgres.postgresqlUsername` | Postgresql username | `"postgres"` |
| `manager.postgres.postgresqlPassword` | Postgresql password | `"hydr0s3rving"` |
| `manager.postgres.postgresqlDatabase` | Postgresql database | `"hydro-serving"` |
| `sonar.javaOpts` | Jvm options | `"-Xmx2048m -Xmn2048m -Xss258k -XX:MaxMetaspaceSize=1024m -XX:+AggressiveHeap"` |
| `sonar.postgres.postgresqlUsername` | Postgresql username | `"postgres"` |
| `sonar.postgres.postgresqlPassword` | Postgresql password | `"hydr0s3rving"` |
| `sonar.postgres.postgresqlDatabase` | Postgresql database | `"hydro-serving"` |
| `sonar.mongo.mongodbUsername` | Mongodb username | `"root"` |
| `sonar.mongo.mongodbPassword` | Mongodb password | `"hydr0s3rving"` |
| `sonar.mongo.mongodbAuthDatabase` | Mongodb auth database | `"admin"` |
| `sonar.mongo.mongodbDatabase` | Mongodb database name | `"hydro-serving-data-profiler"` |
| `docker-registry.proxy.enabled` | Enable docker-registry proxy (use with node-port mode) | `false` |
| `docker-registry.ingress.enabled` | Enable ingress for docker-registry (use with clusterip) | `false` |
