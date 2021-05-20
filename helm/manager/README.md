
Manager
===========

A Helm chart for Kubernetes


## Configuration

The following table lists the configurable parameters of the Manager chart and their default values.

| Parameter                | Description             | Default        |
| ------------------------ | ----------------------- | -------------- |
| `replicaCount` |  | `1` |
| `serviceAccount.create` |  | `true` |
| `global.registry.insecure` | Use insecure docker registry | `true` |
| `global.registry.ingress.enabled` |  | `false` |
| `global.registry.ingress.host` |  | `"hydrosphere-registry.local"` |
| `global.registry.ingress.path` |  | `"/"` |
| `global.registry.ingress.issuer` |  | `"letsencrypt-prod"` |
| `global.registry.url` |  | `""` |
| `global.registry.username` | Username to authenticate to the registry | `"example"` |
| `global.registry.password` | Password to authenticate to the registry | `"example"` |
| `global.postgresql.url` | Postgresql address if external postgresql use | `""` |
| `global.postgresql.username` | Postgresql username | `"postgres"` |
| `global.postgresql.password` | Postgresql password | `"hydr0s3rving"` |
| `global.postgresql.database` | Postgresql database name | `"hydro-serving"` |
| `global.tolerations` |  | `[]` |
| `env` |  | `{}` |
| `monitoring.enabled` |  | `true` |
| `javaOpts` |  | `"-Xmx1024m -Xms128m -Xss16M -Dcom.sun.management.jmxremote.port=5555 -Dcom.sun.management.jmxremote.rmi.port=5555 -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"` |
| `depconfig` |  | `null` |
| `image.full` |  | `"hydrosphere/serving-manager:0f90ed836cc65203ebed399cad48d28c6139d2e0"` |
| `image.pullPolicy` |  | `"IfNotPresent"` |
| `service.type` |  | `"ClusterIP"` |
| `service.http_port` |  | `9090` |
| `service.grpc_port` |  | `9091` |
| `service.jmx_port` |  | `5555` |
| `resources` |  | `{}` |
| `nodeSelector` |  | `{}` |
| `tolerations` |  | `[]` |
| `affinity` |  | `{}` |





