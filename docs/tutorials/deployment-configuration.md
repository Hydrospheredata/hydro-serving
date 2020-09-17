---
description: 'Estimated completion time: 11m.'
---

# Using Deployment Configurations

{% hint style="warning" %}
This tutorial is relevant only for k8s installed Hydrosphere. Please refer to [How to Install Hydrosphere on Kubernetes cluster](../installation/#kubernetes-installation)
{% endhint %}

## Overview

In this tutorial, you will learn how to configure deployed Applications. 

By the end of this tutorial you will know how to:

* Train and upload an example [model version](../overview/concepts.md#models-and-model-versions)
* Create a [Deployment Configuration](../overview/concepts.md#deployment-configurations)
* Create an [Application](../overview/concepts.md#applications) from the uploaded model version with previously created deployment configuration
* Examine settings of a Kubernetes cluster

## Prerequisites

* [Hydrosphere platform installed in Kubernetes cluster](../installation/#kubernetes-installation)
* [Python SDK](../installation/sdk.md#installation) / [CLI](../installation/cli.md#installation)

##  Upload a Model

In this section, we describe the resources required to create and upload an example model used in further sections. If you have no prior experience with uploading models to the Hydrosphere platform we suggest that you visit the [Getting Started Tutorial](../getting-started.md).

Here are the resources used to train `sklearn.ensemble.GradientBoostingClassifier` and upload it to the Hydrosphere cluster.

{% tabs %}
{% tab title="requirements.txt" %}
`requirements.txt` is a list of Python dependencies used during the process of building model image.

```text
numpy~=1.18
scipy==1.4.1
scikit-learn~=0.23
```
{% endtab %}

{% tab title="serving.yaml" %}
`serving.yaml` is a[ resource definition](../overview/concepts.md#resource-definitions) that describes how model should be built and uploaded to Hydrosphere platform.

{% code title="serving.yaml" %}
```yaml
kind: Model
name: my-model
runtime: hydrosphere/serving-runtime-python-3.7:2.3.2
install-command: pip install -r requirements.txt
payload:
  - src/
  - requirements.txt
  - model.joblib
contract:
  name: infer
  inputs:
    x:
      shape: [30]
      type: double
  outputs:
    y:
      shape: scalar
      type: int64
```
{% endcode %}
{% endtab %}

{% tab title="train.py" %}
`train.py` is used to generate a `model.joblib` which is loaded from `func_main.py` during model serving.

Run `python train.py` to generate `model.joblib`

{% code title="train.py" %}
```python
import joblib
import pandas as pd
from sklearn.datasets import make_blobs
from sklearn.ensemble import GradientBoostingClassifier

# initialize data
X, y = make_blobs(n_samples=3000, n_features=30)

# create a model
model = GradientBoostingClassifier(n_estimators=200)
model.fit(X, y)

# Save training data and model
pd.DataFrame(X).to_csv("training_data.csv", index=False)
joblib.dump(model, "model.joblib")
```
{% endcode %}
{% endtab %}

{% tab title="func\_main.py" %}
`func_main.py` is a script which serves requests and produces responses. 

{% code title="func\_main.py" %}
```python
import joblib
import numpy as np

# Load model once
model = joblib.load("/model/files/model.joblib")


def infer(x):
    # Make a prediction
    y = model.predict(x[np.newaxis])

    # Return the scalar representation of y
    return {"y": np.asscalar(y)}
```
{% endcode %}
{% endtab %}
{% endtabs %}

Our folder structure should look like this:

```yaml
dep_config_tutorial
├── model.joblib
├── train.py
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

{% hint style="warning" %}
Do not forget to run `python train.py` to generate `model.joblib`!
{% endhint %}

After we have made sure that all files are placed correctly, we can upload the model to the Hydrosphere platform by running `hs upload` from the command line.

```bash
hs upload
```

## Create a Deployment Configuration

Next, we are going to create and upload an instance of [Deployment Configuration](../overview/concepts.md#deployment-configurations) to the Hydrosphere platform.

Deployment Configurations describe with which Kubernetes settings Hydrosphere should deploy [servables](../overview/concepts.md#servable). You can specify Pod [Affinity](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#affinity-v1-core) and [Tolerations](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#toleration-v1-core), the number of desired pods in deployment, [ResourceRequirements](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#resourcerequirements-v1-core), and Environment Variables for the model container, and [HorizontalPodAutoScaler](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#horizontalpodautoscalerspec-v1-autoscaling) settings.

Created Deployment Configurations can be attached to Servables and Model Variants inside of Application. 

Deployment Configurations are immutable and cannot be changed after they've been uploaded to the Hydrosphere platform. 

You can create and upload Deployment Configuration to Hydrosphere via [YAML Resource definition](../how-to/write-definitions.md#kind-deploymentconfiguration) or via [Python SDK](../installation/sdk.md).

For this tutorial, we'll create a deployment configuration with 2 initial pods per deployment, HPA, and `FOO` environment variable with value `bar`.

{% tabs %}
{% tab title="YAML Resource Definition" %}
Create the deployment configuration resource definition:

{% code title="deployment\_configuration.yaml" %}
```yaml
kind: DeploymentConfiguration
name: my-dep-config
deployment:
  replicaCount: 2
hpa:
  minReplicas: 2
  maxReplicas: 4
  cpuUtilization: 70
container:
  env:
    FOO: bar
```
{% endcode %}

To upload it to the Hydrosphere platform, run:

```bash
hs apply -f deployment_configuration.yaml
```
{% endtab %}

{% tab title="Python SDK" %}
```python
from hydrosdk import Cluster, DeploymentConfigurationBuilder

cluster = Cluster("http://localhost")

dep_config_builder = DeploymentConfigurationBuilder("my-dep-config", cluster)
dep_config = dep_config_builder. \
    with_replicas(replica_count=2). \
    with_env({"FOO":"bar"}). \
    with_hpa(max_replicas=4,
             min_replicas=2,
             target_cpu_utilization_percentage=70).build()
```
{% endtab %}
{% endtabs %}

## Create an Application

{% tabs %}
{% tab title="YAML Resource Definition" %}
Create the application resource definition:

{% code title="application.yaml" %}
```yaml
kind: Application
name: my-app-with-config
pipeline:
  - - model: my-model:1
      weight: 100
      deploymentConfiguartion: my-config
```
{% endcode %}

To upload it to the Hydrosphere platform, run:

```bash
hs apply -f application.yaml
```
{% endtab %}

{% tab title="Python SDK" %}
```python
from application import ApplicationBuilder, ExecutionStageBuilder
from hydrosdk import ModelVersion, Cluster, DeploymentConfiguration

cluster = Cluster('http:\\localhost')
my_model = ModelVersion.find(cluster, "my-model", 1)
my_config = DeploymentConfiguration.find(cluster, "my-config")

stage = ExecutionStageBuilder().with_model_variant(model_version=my_model,
                                                   weight=100,
                                                   deployment_configuration=my_config).build()
                                                   
app = ApplicationBuilder(cluster, "my-app-with-config").with_stage(stage).build()
```
{% endtab %}
{% endtabs %}

## Examine Kubernetes Settings

### Replicas

You can check whether `with_replicas` was successful by calling `kubectl get deployment -A -o wide` and checking the `READY`column.

### HPA

To check whether `with_hpa` was successful you should get a list of all created Horizontal Pod Autoscaler Resources. You can do so by calling `kubectl get hpa -A`

The output is similar to:

```text
NAME                        REFERENCE                                            TARGETS    MINPODS   MAXPODS   REPLICAS   AGE
my-model-1-tumbling-star    CrossVersionObjectReference/my-model-1-tumbling-star 20%/70%    2         4         2          1d
```

### Environment Variables

To list all environment variables run `kubectl exec my-model-1-tumbling-star -it /bin/bash` and then execute the `printenv`  command which prints ann system variables.

The output is similar to:

```text
MY_MODEL_1_TUMBLING_STAR_SERVICE_PORT_GRPC=9091
...
FOO=bar
```

