
Manager
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Manager chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `replicaCount` |  | `1` |
| `serviceAccount.create` |  | `true` |
| `global.ingress.enabled` |  | `false` |
| `global.registry.internal` | Use internal registry or external (for internal - ingress required) | `true` |
| `global.registry.insecure` | Use insecure docker registry | `true` |
| `global.registry.url` | Domain name to internal\external registry | `"index.docker.io"` |
| `global.registry.username` | Internal\external registry username | `"example"` |
| `global.registry.password` | Internal\external registry password | `"example"` |
| `global.postgresql.url` | Postgresql address if external postgresql use | `""` |
| `global.postgresql.username` | Postgresql username | `"postgres"` |
| `global.postgresql.password` | Postgresql password | `"hydr0s3rving"` |
| `global.postgresql.database` | Postgresql database name | `"hydro-serving"` |
| `global.tolerations` |  | `[]` |
| `env` |  | `{}` |
| `image.full` |  | `"hydrosphere/serving-manager:bd4f5fc6febcaf707de8bfb186008b236455540e"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





