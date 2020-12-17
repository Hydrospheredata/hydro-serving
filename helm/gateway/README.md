
Gateway
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Gateway chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.tolerations` |  | `[]` |
| `replicaCount` |  | `1` |
| `image.full` |  | `"hydrosphere/serving-gateway:3ae75a74213c57273192da6406b14602319c4b9c"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





