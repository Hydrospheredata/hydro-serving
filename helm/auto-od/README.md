Auto-od
===========

hydro-auto-od service is responsible for creating monitoring metrics for deployed machine learning models via unsuperviesd AutoML techniques.

Each time a new model version is uploaded to the cluster, sonar service calls hydro-auto-od service by the /auto_metric endpoint. This launches a process of creating a metric for monitoring this new model.


## Configuration

The following table lists the configurable parameters of the Auto-od chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `image.full` |  | `"docker.hydrosphere.io/auto-od:ada5baddefde9544fcc472463b98def2d050be7b"` |
| `image.pullPolicy` | Ifnotpresent | `"Always"` |
| `global.awsCredentialSecretName` |  | `""` |
| `global.mongodb.usePassword` |  | `true` |
| `global.mongodb.mongodbRootPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbUsername` |  | `"root"` |
| `global.mongodb.mongodbPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbAuthDatabase` |  | `"admin"` |
| `global.mongodb.mongodbDatabase` |  | `"hydro-serving-data-profiler"` |
| `service.type` |  | `"ClusterIP"` |
| `service.port` |  | `5001` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---