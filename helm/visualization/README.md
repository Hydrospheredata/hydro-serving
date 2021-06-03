
Visualization
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Visualization chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `image.full` |  | `"hydrosphere/hydro-visualization:94f27b119de749cc646d9faa3bedb83160d25002"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `persistence.bucket` |  | `"hydrosphere-visualization-artifacts"` |
| `global.persistence.url` |  | `""` |
| `global.persistence.mode` |  | `"minio"` |
| `global.persistence.accessKey` |  | `"ACCESSKEYEXAMPLE"` |
| `global.persistence.secretKey` |  | `"SECRETKEYEXAMPLE"` |
| `global.persistence.region` |  | `"us-east-1"` |
| `global.mongodb.url` | Mongodb address if external mongodb use | `""` |
| `global.mongodb.rootPassword` | Mongodb root password | `"hydr0s3rving"` |
| `global.mongodb.username` | Mongodb username | `"root"` |
| `global.mongodb.password` | Mongodb password | `"hydr0s3rving"` |
| `global.mongodb.authDatabase` | Mongodb auth database | `"admin"` |
| `global.mongodb.database` | Mongodb database name | `"hydro-serving-data-profiler"` |
| `global.tolerations` |  | `[]` |
| `service.type` |  | `"ClusterIP"` |
| `service.httpPort` |  | `5000` |
| `service.grpcPort` |  | `5003` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





