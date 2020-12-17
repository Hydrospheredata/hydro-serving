
Ui
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Ui chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `replicaCount` |  | `1` |
| `public` |  | `true` |
| `sonar.enabled` |  | `false` |
| `global.ingress.enabled` |  | `false` |
| `global.ingress.host` | Domain name to frontend | `"ui.hydrosphere.io"` |
| `global.ingress.issuer` | Cert-manager issuer name | `"letsencrypt-prod"` |
| `global.ingress.path` |  | `"/"` |
| `global.ingress.enableGrpc` |  | `true` |
| `global.tolerations` |  | `[]` |
| `image.full` |  | `"hydrosphere/hydro-serving-ui:eba6295a96703e940a7c946c4961e2ec740011f4"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.httpPort` |  | `9090` |
| `service.grpcPort` |  | `9091` |
| `ingress.enabled` |  | `false` |
| `configuration` |  | `""` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





