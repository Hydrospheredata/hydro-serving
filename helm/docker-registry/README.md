Docker-registry
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Docker-registry chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.dockerRegistry.host` |  | `"hydro-serving.local"` |
| `global.dockerRegistry.username` |  | `"developers"` |
| `global.dockerRegistry.password` |  | `"hydr0s3rving"` |
| `global.persistence.enabled` |  | `false` |
| `global.s3.accessKey` |  | `"ACCESSKEYEXAMPLE"` |
| `global.s3.secretKey` |  | `"SECRETKEYEXAMPLE"` |
| `global.s3.region` |  | `"eu-central-1"` |
| `global.s3.bucket` |  | `"docker-registry-dev-hydrosphere"` |
| `replicaCount` |  | `1` |
| `proxy.enabled` |  | `true` |
| `ingress.enabled` |  | `false` |
| `image.repository` |  | `"registry"` |
| `image.tag` |  | `"2.6.2"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.port` |  | `5000` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---