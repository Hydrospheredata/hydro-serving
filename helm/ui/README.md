Ui
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Ui chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `replicaCount` |  | `1` |
| `sonar.enabled` |  | `false` |
| `image.full` |  | `"hydrosphere/serving-manager-ui:e99a3feb0607d033bf6482c183ff73bd492d2bdc"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.httpPort` |  | `9090` |
| `service.grpcPort` |  | `9091` |
| `ingress.enabled` |  | `false` |
| `ingress.annotations` |  | `{}` |
| `ingress.path` |  | `"/"` |
| `ingress.hosts` |  | `["hydro-serving.local"]` |
| `ingress.tls` |  | `[]` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---