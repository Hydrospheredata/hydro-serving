Visualization
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Visualization chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `image.full` |  | `"docker.hydrosphere.io/visualization:054370b455002511c1387e33f1252baa472ed3fe"` |
| `image.pullPolicy` | Ifnotpresent | `"Always"` |
| `global.awsCredentialSecretName` |  | `""` |
| `global.mongodb.usePassword` |  | `true` |
| `global.mongodb.mongodbRootPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbUsername` |  | `"root"` |
| `global.mongodb.mongodbPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbAuthDatabase` |  | `"admin"` |
| `global.mongodb.mongodbDatabase` |  | `"hydro-serving-data-profiler"` |
| `global.s3.featureLakeBucketName` |  | `"feature-lake"` |
| `global.s3.featureLakeBucketCreate` |  | `false` |
| `global.s3.hydrovizBucketName` |  | `"hydro-viz"` |
| `service.type` |  | `"ClusterIP"` |
| `service.httpPort` |  | `5000` |
| `service.grpcPort` |  | `5003` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---