Gateway
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Gateway chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.shadowing_on` |  | `false` |
| `replicaCount` |  | `1` |
| `image.full` |  | `"hydrosphere/serving-gateway:7efc404a2877ce9d52eb393593f539225fd691b2"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---