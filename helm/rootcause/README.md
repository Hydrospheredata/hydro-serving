
Rootcause
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Rootcause chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.persistence.url` | Endpoint for the object storage. compatible with s3 or minio. | `""` |
| `global.persistence.mode` |  | `"minio"` |
| `global.persistence.accessKey` | Accesskeyid for s3 or minio | `"ACCESSKEYEXAMPLE"` |
| `global.persistence.secretKey` | Secretkeyid for s3 or minio | `"SECRETKEYEXAMPLE"` |
| `global.persistence.region` |  | `"us-east-1"` |
| `global.tolerations` |  | `[]` |
| `global.mongodb.url` | Mongodb address if external mongodb use | `""` |
| `global.mongodb.rootPassword` | Mongodb root password | `"hydr0s3rving"` |
| `global.mongodb.username` | Mongodb username | `"root"` |
| `global.mongodb.password` | Mongodb password | `"hydr0s3rving"` |
| `global.mongodb.authDatabase` | Mongodb auth database | `"admin"` |
| `global.mongodb.database` | Mongodb database name | `"rootcause"` |
| `image.full` |  | `"hydrosphere/hydro-root-cause:b3ac139fa54bd4b09a4059110dd74ed51880ef34"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.port` |  | `5005` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





