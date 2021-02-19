
Manager
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Manager chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `replicaCount` |  | `1` |
| `serviceAccount.create` |  | `true` |
| `global.registry.insecure` | Use insecure docker registry | `true` |
| `global.registry.ingress.enabled` |  | `false` |
| `global.registry.ingress.host` |  | `"hydrosphere-registry.local"` |
| `global.registry.ingress.path` |  | `"/"` |
| `global.registry.ingress.issuer` |  | `"letsencrypt-prod"` |
| `global.registry.url` |  | `""` |
| `global.registry.username` | Username to authenticate to the registry | `"example"` |
| `global.registry.password` | Password to authenticate to the registry | `"example"` |
| `global.postgresql.url` | Postgresql address if external postgresql use | `""` |
| `global.postgresql.username` | Postgresql username | `"postgres"` |
| `global.postgresql.password` | Postgresql password | `"hydr0s3rving"` |
| `global.postgresql.database` | Postgresql database name | `"hydro-serving"` |
| `global.tolerations` |  | `[]` |
| `env` |  | `{}` |
| `javaOpts` |  | `"-Xmx1024m -Xms128m -Xss16M"` |
| `image.full` |  | `"hydrosphere/serving-manager:0c029174cf73e847906cfa21395d0334bca69582"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





