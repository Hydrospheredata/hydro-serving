
Stat
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Stat chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `image.full` |  | `"hydrosphere/stat:5b5bc87c010abbd0787b234c3e195564ce31c6b6"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `global.persistence.mode` | Enable persistence mode for services (use s3, minio, local) | `"local"` |
| `global.persistence.accessKey` | Accesskeyid for s3 or minio | `"ACCESSKEYEXAMPLE"` |
| `global.persistence.secretKey` | Secretkeyid for s3 or minio | `"SECRETKEYEXAMPLE"` |
| `global.persistence.region` | S3 bucket region | `"eu-central-1"` |
| `global.persistence.endpointUrl` | Endpoint for non aws s3 | `"http://minio:9000"` |
| `global.persistence.bucket` | S3 bucket name | `"example"` |
| `global.mongodb.url` | Mongodb address if external mongodb use | `""` |
| `global.mongodb.rootPassword` | Mongodb root password | `"hydr0s3rving"` |
| `global.mongodb.username` | Mongodb username | `"root"` |
| `global.mongodb.password` | Mongodb password | `"hydr0s3rving"` |
| `global.mongodb.authDatabase` | Mongodb auth database | `"admin"` |
| `global.mongodb.database` | Mongodb database name | `"hydro-serving-data-profiler"` |
| `global.tolerations` |  | `[]` |
| `service.type` |  | `"ClusterIP"` |
| `service.port` |  | `5002` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





