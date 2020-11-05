
Serving
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Serving chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.ingress.enabled` |  | `false` |
| `global.ingress.host` | Domain name for the frontend ingress. | `"ui.example.io"` |
| `global.ingress.path` | Path, which will match the service. | `"/"` |
| `global.ingress.enableGrpc` | Enable grpc endpoints for services. works only with `path: "/"`. | `true` |
| `global.ingress.issuer` | A name of the cert-manager issuer name, configured within the | `"letsencrypt-prod"` |
| `global.registry.internal` | Internal/external mode for the registry. in case of internal registry | `true` |
| `global.registry.insecure` | Use insecure docker registry | `true` |
| `global.registry.url` | Domain name for internal\external registry. in case | `"index.docker.io/username"` |
| `global.registry.username` | Internal\external registry username | `"example"` |
| `global.registry.password` | Internal\external registry password | `"example"` |
| `global.hydrosphere.docker.host` |  | `"harbor.hydrosphere.io/hydro-serving"` |
| `global.hydrosphere.docker.username` |  | `"developers"` |
| `global.hydrosphere.docker.password` | Registry password for accessing closed images | `""` |
| `global.persistence.mode` | Persistence mode for services (one of s3, minio) | `"minio"` |
| `global.persistence.accessKey` | Accesskeyid for s3 or minio | `"ACCESSKEYEXAMPLE"` |
| `global.persistence.secretKey` | Secretkeyid for s3 or minio | `"SECRETKEYEXAMPLE"` |
| `global.persistence.region` | Region of the bucket in case of s3 persistence mode. | `"eu-central-1"` |
| `global.persistence.bucket` | S3 bucket name in case of s3 persistence mode. | `"example"` |
| `global.mongodb.url` | Mongodb host in case of using external mongodb instance. if not specified, | `""` |
| `global.mongodb.rootPassword` | Mongodb root password | `"hydr0s3rving"` |
| `global.mongodb.username` | Mongodb username | `"root"` |
| `global.mongodb.password` | Mongodb password | `"hydr0s3rving"` |
| `global.mongodb.authDatabase` | Mongodb auth database | `"admin"` |
| `global.mongodb.database` | Mongodb database name | `"hydro-serving-data-profiler"` |
| `global.postgresql.url` | Postgresql host in case of using external postgresql instance. if not specified, | `""` |
| `global.postgresql.username` | Postgresql username | `"postgres"` |
| `global.postgresql.password` | Postgresql password | `"hydr0s3rving"` |
| `global.postgresql.database` | Postgresql database name | `"hydro-serving"` |
| `global.alertmanager.url` | Alertmanager address if external use (address:port). if not specified, | `""` |
| `global.tolerations` |  | `[]` |
| `prometheus-am.config.global.smtp_smarthost` | Smtp relay host | `"localhost:25"` |
| `prometheus-am.config.global.smtp_auth_username` | Smtp relay username | `"mailbot"` |
| `prometheus-am.config.global.smtp_auth_identity` | Smtp relay username identity | `"mailbot"` |
| `prometheus-am.config.global.smtp_auth_password` | Smtp relay password | `"mailbot"` |
| `prometheus-am.config.global.smtp_from` | Email address of the sender | `"no-reply@hydrosphere.io"` |
| `prometheus-am.config.route.group_by` |  | `["alertname", "modelVersionId"]` |
| `prometheus-am.config.route.group_wait` |  | `"10s"` |
| `prometheus-am.config.route.group_interval` |  | `"10s"` |
| `prometheus-am.config.route.repeat_interval` |  | `"1h"` |
| `prometheus-am.config.route.receiver` |  | `"default"` |
| `prometheus-am.config.receivers` |  | `[{"name": "default", "email_configs": [{"to": "customer@example.io"}]}]` |
| `ui.public` |  | `false` |





