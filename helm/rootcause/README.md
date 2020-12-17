Rootcause
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Rootcause chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `global.mongodb.usePassword` |  | `true` |
| `global.mongodb.mongodbRootPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbUsername` |  | `"root"` |
| `global.mongodb.mongodbPassword` |  | `"hydr0s3rving"` |
| `global.mongodb.mongodbAuthDatabase` |  | `"admin"` |
| `global.mongodb.mongodbDatabase` |  | `"rootcause"` |
| `global.awsCredentialSecretName` |  | `""` |
| `image.full` |  | `"docker.hydrosphere.io/hydro-root-cause:fcafdc4795ac37cdeb9533054771c806966aa264"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.port` |  | `5005` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |



---