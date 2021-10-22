# Prerequisite

* AWS account
* EKS cluster 1.18 or greater

# Registries

AWS has a service Elastic Container Registries, but this service can't auto-create repository on push. We can't use it, to store our serving docker image.

You can use an external registry, like a DockerHub, Artifactory, VMWare Harbour, or your own registry. Also, you could use AWS marketplace to find registries solutions or just install a container registry with the certificate on EC2.

## Set registry in helm

After uploading the model, hydro-serving-manager make docker image and store it in the registry (path https://urlregistry.example.com/modelname:modelversion) and kubelet service should be able to download it.
In `values.yaml` or `values-production.yaml` set

```yaml
  registry:
    insecure: false
    url: "docker.io"  # Example for dockerhub
    username: example # Username to authenticate to the registry 
    password: example # Password to authenticate to the registry
```

# Databases

We used 2 databases, PostgreSQL 10 or greater and MongoDB 4 or greater.

For AWS you can set up RDS PostgreSQL or AuroraDB with database engine postgresql:10 or greater

For MongoDB, you can use AWS marketplace or install it on EC2. DocumentDB doesn't work now.

## Create postgres RDS instance
```Shell
aws rds create-db-instance \ 
  --db-instance-identifier hydrosphere-instance \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username root \
  --master-user-password password \
  --allocated-storage 100
```

After creating the instance, connect to the RDS instance and create a new database with additional params like a maintenance window, backup options, etc.
## Set RDS and MongoDB in helm

```yaml
  mongodb:
    url: "mongodb.example.com:27017"                    # Specify MongoDB connection string if you want to use an external MongoDB instance. 
    rootPassword: hydr0s3rving                          # Specify root password
    username: root                                      # Specify username
    password: hydr0s3rving                              # Specify user password
    authDatabase: admin                                 # Specify authDatabase
    retry: false                                        # Specify retry options if it's required
    database: hydro-serving-data-profiler               # Specify mongoDB database

  postgresql:
    url: "hydrosphere-instance.us-east-1.amazonaws.com" # Specify Postgresql connection string if you want to use an external Postgresql instance. 
    username: root                                      # Specify root username
    password: hydr0s3rving                              # Specify root password
    database: hydro-serving                             # Specify database name

```

# Persistence

We used s3 to store training data, models metrics, and docker registry storage.

You can use 1 bucket for all services, they will create a path or separate buckets for hydro-vizualisations and sonar.

We used minio as an s3 proxy or you can use any s3 like object storage. To use your own, specify url in the persistence block

```yaml
  persistence:
    url: ""                     # Any s3 like object storage
    accessKey: ACCESSKEYEXAMPLE # accesskeyid for s3 or minio
    secretKey: SECRETKEYEXAMPLE # secretkeyid for s3 or minio
```

If you want to use s3, set s3 and credentials:

```yaml
  persistence:
    mode: s3                    # Defines the type of the persistence storage. Valid options are "s3" and "minio".
    accessKey: ACCESSKEYEXAMPLE # accesskeyid for s3 or minio
    secretKey: SECRETKEYEXAMPLE # secretkeyid for s3 or minio
    region: us-east-1
```

In that case, minio will be installed with s3 backend mode, and work as an s3 gateway.

All services try to create s3 buckets if they don't exist. By default:

<b>sonar</b> will create `hydrosphere-feature-lake`

<b>vizualization</b> will create `hydrosphere-visualization-artifacts`

<b>docker-registry</b> will create `hydrosphere-model-registry`

# Tolerations

Some cases required a different environments. You could use different machine groups for different installations, or just be sure, that hydrosphere installs only some type of nodes.

Tolerations help you set these rules.

```yaml
  tolerations:
     - key: node
       operator: Equal
       value: highPerformance
       effect: NoSchedule  
```

This example configure all deployments to deploy only nodes with taint `node: highPerformance`

More informations about tolerations [here](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/)


# Resource limits

All our helm charts have resource params and java services have javaOpts. These params help to configure requests and limits resources, also javaOpts help tune JVM machine.

resources set pod requests and limits params for CPU and memory.


```yaml
  resources:
    limits:
      memory: 4Gi
      cpu: 1
    requests:
      memory: 512Mi
      cpu: 1
```

More info about [resources](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)