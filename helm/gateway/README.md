
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
| `image.full` |  | `"hydrosphere/serving-gateway:4fcc79009505954451867b489eae18cd78dd4e25"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





