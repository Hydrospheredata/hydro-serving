Stat
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Stat chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `image.full` |  | `"docker.hydrosphere.io/stat:84f33c817748c2f4a2fbc99e5c476076f887052f"` |
| `image.pullPolicy` | Ifnotpresent | `"Always"` |
| `global.awsCredentialSecretName` |  | `""` |
| `global.mongodb.usePassword` |  | `true` |
| `global.mongodb.mongodbRootPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbUsername` |  | `"root"` |
| `global.mongodb.mongodbPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbAuthDatabase` |  | `"admin"` |
| `global.mongodb.mongodbDatabase` |  | `"hydro-serving-data-profiler"` |
| `service.type` |  | `"ClusterIP"` |
| `service.port` |  | `5002` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---