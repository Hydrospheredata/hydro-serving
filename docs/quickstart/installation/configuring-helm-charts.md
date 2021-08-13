# Configuring Helm charts

This article explains the configuration file of the Hydrosphere Helm charts.

## Prerequisistes

To install Hydrosphere on the Kubernetes cluster you should have the following prerequisites fulfilled.

* [Helm 3.0+](https://docs.helm.sh/using_helm/#install-helm)
* [Kubernetes 1.16+ with v1 API](https://kubernetes.io/docs/setup/)
* PV support on the underlying infrastructure \(if persistence is required\)
* Docker registry with pull/push access \(if the built-in one is not used\)

## Configuring Helm charts

Fetch the newest charts to your local directory.

1. Add the Hydrosphere charts repository:

   ```bash
   helm repo add hydrosphere https://hydrospheredata.github.io/hydro-serving/helm
   ```

2. Install the chart from repo to the cluster:

   ```bash
   helm fetch --untar hydrosphere/serving
   cd serving
   ```

Helm charts are bundled with two distinct configuration files. The default one is `values.yaml`, the more comprehensive one is `values-production.yaml`.

By default \(in the `values.yaml`\), Helm charts are configured to set up a basic Hydrosphere installation aimed for a **testing** workload. To configure the installation for the **production** workload you have to set up additional resources, such as separate database instances, a separate docker registry, and override default values in the configuration file.

The contents of `values.yaml` and `values-production.yaml` files are overlapping, so we will continue with the latter.

## Structure of `values-production.yaml`

```yaml
global:
  ui:
    ingress:
      enabled: false
      host: hydrosphere.local
      path: "/"
      enableGrpc: true # Enable ingress resources for grpc endpoints for services. Works only with `path: "/"`. 
      issuer: letsencrypt-prod

  registry:
    insecure: true
    ingress: # optional, when url != ""
      enabled: false
      host: hydrosphere-registry.local
      path: "/"
      issuer: letsencrypt-prod
    url: ""
    username: example # Username to authenticate to the registry 
    password: example # Password to authenticate to the registry
    persistence: # optional, when url != ""
      bucket: hydrosphere-model-registry
      region: us-east-1

  persistence:
    url: ""
    mode: minio # Defines the type of the persistence storage. Valid options are "s3" and "minio".
    accessKey: ACCESSKEYEXAMPLE # accesskeyid for s3 or minio
    secretKey: SECRETKEYEXAMPLE # secretkeyid for s3 or minio
    region: us-east-1 # optional, when mode == "minio"

  mongodb:
    url: "" # Specify MongoDB connection string if you want to use an external MongoDB instance. 
            # If empty, an in-cluster deployment will be provisioned. 
    rootPassword: hydr0s3rving 
    username: root 
    password: hydr0s3rving
    authDatabase: admin
    retry: false
    database: hydro-serving-data-profiler

  postgresql:
    url: "" # Specify Postgresql connection string if you want to use an external Postgresql instance. 
            # If empty, an in-cluster deployment will be provisioned.
    username: postgres
    password: hydr0s3rving
    database: hydro-serving

  alertmanager:
    url: "" # Prometheus AlertManager address in case you want to use the external installation.
            # If empty, an internal installation will be deployed.
    config:
      global: 
        smtp_smarthost: localhost:25 # SMTP relay host
        smtp_auth_username: mailbot # SMTP relay username 
        smtp_auth_identity: mailbot # SMTP relay username identity
        smtp_auth_password: mailbot # SMTP relay password
        smtp_from: no-reply@hydrosphere.io # Email address of the sender
      route:
        group_by: [alertname, modelVersionId]
        group_wait: 10s
        group_interval: 10s
        repeat_interval: 1h
        receiver: default
      receivers:
      - name: default
        email_configs: # List of email addresses to send alarms to
        - to: customer@example.io

  tolerations: []
    # - key: key
    #   operator: Equal
    #   value: value
    #   effect: NoSchedule  

ui:
  resources: {}

manager:
  javaOpts: "-Xmx1024m -Xms128m -Xss16M"
  servingAccount:
    create: true
    # name: "hydro-serving-manager-sa"
  resources: {}

gateway:
  javaOpts: "-Xmx512m -Xms64m -Xss16M"
  resources: {}

sonar:
  # A service, responsible for managing metrics, managing training and production data storage,
  # calculating profiles, and shadowing data to the monitoring metrics. 
  javaOpts: "-Xmx2048m -Xmn2048m -Xss258k -XX:MaxMetaspaceSize=1024m -XX:+AggressiveHeap"
  persistence:
    bucket: "hydrosphere-feature-lake"
    region: "us-east-1"

  resources:
    limits:
      memory: 4Gi
    requests:
      memory: 512Mi

auto-od:
  # A service, responsible for automatically generating outlier detection metrics for your 
  # production models based on the training data of the model. 
  resources: {}

stat:
  # A service, responsible for creating statistical reports for your production models based
  # on a comparison of training and production data distributions. Compares these two sets 
  # of data by a set of statistical tests and finds deviations.
  resources: {}

vizualization:
  # A service, responsible for visualizing high-dimensional data in a 2D scatter plot with
  # an automatically trained transformer to let you evaluate the data structure and spot 
  # clusters, outliers, novel data, or any other patterns. This is especially helpful if 
  # your model works with high-dimensional data, such as images or text embeddings. 
  persistence:
    bucket: hydrosphere-visualization-artifacts
    region: us-east-1
  resources: {}

rootcause:
  # A service, responsible for generating explanations for a particular model prediction to 
  # help you understand the outcome by telling why your model made the prediction. 
  resources: {}

# Pull secret for hydrosphere from private registry
registry:
  enabled: false
  host: "" # Registry url for accessing hydrosphere images
  username: "" # Registry username for accessing hydrosphere images
  password: "" # Registry password for accessing hydrosphere images
```

Let's go over each section one by one.

### UI

`.global.ui.ingress.enabled` is responsible for creating an ingress resource for the HTTP endpoint of the UI service.

`.global.ui.ingress.host` specifies the DNS name of the ingress resource.

`.global.ui.ingress.path` specifies the context path of the ingress resource.

`.global.ui.ingress.enableGrpc` is responsible for creating an ingress resource for the GRPC endpoint of the UI service. Note, specifying `.global.ui.ingress.enableGrpc: true` only works when the path is set to "/", so it's recommended to leave `.global.ui.ingress.path` untouched.

`.global.ui.ingress.issuer` is the name of the configured certificate issuer for ingress resources. Make sure it's set to either an Issuer or a ClusterIssuer. We do not bundle certificate manager to the Hydrosphere charts, so you have to set up this yourself. Consider consulting [cert-manager.io](https://cert-manager.io/docs/) documentation for more help.

`.ui.resources` section specifies resource requests and limits for the service.

### Docker Registry

{% hint style="info" %}
It is recommended to use a preconfigured docker registry for the **production** workload.

If you do not specify `.global.registry.url,`Hydrosphere will create an internal instance of the docker registry. This approach is only recommended for **testing** purposes.
{% endhint %}

`.global.registry.url` specifies the endpoint of your preconfigured docker registry.

`.global.registry.username` and `.global.registry.password` specify the credentials for your registry.

`.global.registry.ingress.enabled` is responsible for creating an ingress resource for the registry service. This also issues certificates for the docker registry, which are required for external registries.

If `.global.registry.ingress.enabled` is set to "true", `.global.registry.insecure` should be set to "false". This will tell Hydrosphere to work with the registry in **secure** mode.

If `.global.registry.ingress.enabled` is set to "false"_,_ `.global.registry.insecure` _\_should be set to "true"_.\_ This will tell Hydrosphere to work with the registry in **insecure** mode. This will also create a DaemonSet which will proxy all requests to the registry from each node.

`.global.registry.persistence` section configures persistency options for the service. This is only valid when `.global.persistence.mode` is set to "s3".

`.global.registry.persistence.bucket` specifies the bucket name, where to store images.

`.global.registry.persistence.region` specifies region of the bucket. If not specified, it will be fallback to `.global.persistence.region`.

### Persistence

{% hint style="info" %}
It is recommended to use a preconfigured persistent storage for the **production** workload.

If you do not specify `.global.persistence.url`, Hydrosphere will create an internal instance of the minio storage. This approach is only recommended for **testing** purposes.
{% endhint %}

`.global.persistence.url` specifies the endpoint for your preconfigured storage.

`.global.persistence.mode` specifies, which persistence mode is used. Only valid options are "s3" or "minio".

`.global.persistence.accessKey` and `.global.persistence.secretKey` specify credentials to the storage.

`.global.persistence.region` specifies default regional constraint for the buckets.

Internal instance can be created when `.global.persistence.mode` is set to "minio".

### MongoDB

{% hint style="info" %}
It is recommended to use a preconfigured Mongo database instance for the **production** workload. `.global.mongodb.url` specifies the endpoint for your preconfigured Mongo instance.

If you omit specifying `.global.mongodb.url`, Hydrosphere will create an internal instance of the MongoDB database. This approach is only recommended for **testing** purposes.
{% endhint %}

### Postgresql

{% hint style="info" %}
It is recommended to use a preconfigured PostgreSQL database instance for the **production** workload. `.global.postgresql.url` specifies the endpoint for your preconfigured PostgreSQL instance.

If you omit specifying `.global.postgresql.url`, Hydrosphere will create an internal instance of the PostgreSQL database. This approach is only recommended for **testing** purposes.
{% endhint %}

### AlertManager

`.global.alertmanager.url` specifies the endpoint for your preconfigured Prometheus AlertManager instance. If you omit specifying it, Hydrosphere will create an internal instance of AlertManager.

`.global.alertmanager.config` specifies configuration file for the AlertManager. Consider consulting [AlertManager](https://prometheus.io/docs/alerting/latest/configuration/) documentation for more details.

### Manager

You can learn more about the Manager service in the [Serving](../../about/services/serving.md#manager) section.

`.manager.javaOpts` specifies Java options for the service.

`.manager.serviceAccount` section specifies ServiceAccount details for Manager service to use, when managing Kubernetes resources.

`.manager.resources` section specifies resource requests and limits for the service.

### Gateway

You can learn more about the Gateway service in the [Serving](../../about/services/serving.md#gateway) section.

`.gateway.javaOpts` specifies Java options for the service.

`.gateway.resources` section specifies resource requests and limits for the service.

### Sonar

You can learn more about the Sonar service in the [Monitoring](../../about/services/monitoring.md#sonar) section.

`.sonar.javaOpts` specifies Java options for the service.

`.sonar.persistence` section configures persistency options for the service.

`.sonar.persistence.bucket` specifies the bucket name, where to store training data and other artifacts.

`.sonar.persistence.region` specifies region of the bucket. If not specified, it will be fallback to `.global.persistence.region`.

`.sonar.resources` section specifies resource requests and limits for the service.

### AutoOD

You can learn more about the AutoOd service in the [Monitoring](../../about/services/monitoring.md#automatic-outlier-detection) section.

`.auto-od.resources` section specifies resource requests and limits for the service.

### Stat

You can learn more about the Stat service in the [Monitoring](../../about/services/monitoring.md#drift-report) section.

`.stat.resources` section specifies resource requests and limits for the service.

### Visualization

You can learn more about the Visualization service in the [Interpretability](../../about/services/interpretability.md#data-projections) section.

`.visualization.persistence` section configures persistency options for the service.

`.visualization.persistence.bucket` specifies the bucket name, where to store data artifacts.

`.visualization.persistence.region` specifies region of the bucket. If not specified, it will be fallback to `.global.persistence.region`.

`.visualization.resources` section specifies resource requests and limits for the service.

### RootCause

You can learn more about the RootCause service in the [Interpretability](../../about/services/interpretability.md#prediction-explanations) section.

`.rootcause.resources` section specifies resource requests and limits for the service.

**Tolerations**

You can specify global tolerations for Hydrosphere services to be deployed on particular nodes using `.global.tolerations`. Consider consulting [Kubernetes](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/) documentation for more details.

## Installing charts

Once the charts were configured, install the release.

```bash
helm2 install serving --namespace hydrosphere -f values-production.yaml .
```

