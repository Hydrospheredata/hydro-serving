
Gateway
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Gateway chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.tolerations` |  | `[]` |
| `replicaCount` |  | `1` |
| `JAVA_XMX` |  | `"1G"` |
| `image.full` |  | `"hydrosphere/serving-gateway:3126a9b59a7758402173efc38970d67e8fbb8d3c"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





