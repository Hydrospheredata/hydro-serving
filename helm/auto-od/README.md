
Auto-od
===========

A Helm chart for hydro-auto-od service is responsible for creating monitoring metrics for deployed machine learning models via unsuperviesd AutoML techniques.


## Configuration

The following table lists the configurable parameters of the Auto-od chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `image.full` |  | `"hydrosphere/auto-od:8e45e6661db4225fa27e5fb44af0acfc5b0aaac0"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `global.persistence.url` | Endpoint for the object storage. compatible with s3 or minio. | `""` |
| `global.persistence.mode` |  | `"minio"` |
| `global.persistence.accessKey` | Accesskeyid for s3 or minio | `"ACCESSKEYEXAMPLE"` |
| `global.persistence.secretKey` | Secretkeyid for s3 or minio | `"SECRETKEYEXAMPLE"` |
| `global.persistence.region` |  | `"us-east-1"` |
| `global.mongodb.url` | Mongodb address if external mongodb use | `""` |
| `global.mongodb.rootPassword` | Mongodb root password | `"hydr0s3rving"` |
| `global.mongodb.username` | Mongodb username | `"root"` |
| `global.mongodb.password` | Mongodb password | `"hydr0s3rving"` |
| `global.mongodb.authDatabase` | Mongodb auth database | `"admin"` |
| `global.mongodb.database` | Mongodb database name | `"hydro-serving-data-profiler"` |
| `global.tolerations` |  | `[]` |
| `service.type` |  | `"ClusterIP"` |
| `service.port` |  | `5001` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





