
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
| `image.full` |  | `"harbor.hydrosphere.io/hydro-serving/hydro-serving-ui:5268d5c7321b0fada4012fb1d07b70f42683a5c2"` |
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





