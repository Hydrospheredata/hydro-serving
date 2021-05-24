
Ui
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Ui chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `replicaCount` |  | `1` |
| `global.ui.ingress.enabled` |  | `false` |
| `global.ui.ingress.host` | Domain name for the frontend ingress. | `"hydrosphere.local"` |
| `global.ui.ingress.path` | Path, which will match the service. | `"/"` |
| `global.ui.ingress.enableGrpc` | Enable grpc endpoints for services. works only with `path: "/"`. | `true` |
| `global.ui.ingress.grpchost` | Domain name for the grpc ingress | `"grpc.local"` |
| `global.ui.ingress.issuer` | A name of the cert-manager issuer name, configured within the | `"letsencrypt-prod"` |
| `global.ui.configuration` |  | `"{\n  \"showHeader\": true\n}\n"` |
| `global.tolerations` |  | `[]` |
| `image.full` |  | `"hydrosphere/hydro-serving-ui:5d55e1a62f1911d6329d139c0584e179691d0402"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.httpPort` |  | `9090` |
| `service.grpcPort` |  | `9091` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





