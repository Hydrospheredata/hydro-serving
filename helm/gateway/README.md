
Gateway
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Gateway chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.tolerations` |  | `[]` |
| `replicaCount` |  | `1` |
| `javaOpts` |  | `"-Xmx512m -Xms64m -Xss16M"` |
| `image.full` |  | `"hydrosphere/serving-gateway:98e9add3a034ef5b85d628bb17cbceec1bfd515c"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





