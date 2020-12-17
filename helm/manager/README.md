Manager
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Manager chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `replicaCount` |  | `1` |
| `serviceAccount.create` |  | `true` |
| `global.dockerRegistry.host` |  | `""` |
| `global.dockerRegistry.username` |  | `""` |
| `global.dockerRegistry.password` |  | `""` |
| `global.elasticsearch.host` |  | `""` |
| `postgres.postgresqlUsername` |  | `"postgres"` |
| `postgres.postgresqlPassword` |  | `"hydr0s3rving"` |
| `postgres.postgresqlDatabase` |  | `"hydro-serving"` |
| `env` |  | `{}` |
| `image.full` |  | `"hydrosphere/serving-manager:18a7bb77bdf9c39d97ff30882dec8275ab71c6e6"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---