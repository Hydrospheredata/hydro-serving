# Using Deployment Configurations

{% hint style="warning" %}
This tutorial is relevant only for k8s installed Hydrosphere. Please refer to [How to Install Hydrosphere on Kubernetes cluster](../installation/#kubernetes-installation)
{% endhint %}

## Upload Model

```yaml
dep_config_tutorial
├── model.joblib
├── train.py
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

{% tabs %}
{% tab title="requirements.txt" %}


```text
numpy~=1.18
scipy==1.4.1
scikit-learn~=0.23
```
{% endtab %}

{% tab title="serving.yaml" %}


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

## Create Deployment Configuration

{% code title="dep\_config.yaml" %}
```text
kind: DeploymentConfiguration
name: my-dep-config
...
```
{% endcode %}

```python
from hydrosdk import Cluster, DeploymentConfigurationBuilder

cluster = Cluster("http://localhost")

dep_config_builder = DeploymentConfigurationBuilder("my-dep-config", cluster)
dep_config = dep_config_builder. \
    with_replicas(replica_count=2). \
    with_hpa(max_replicas=4,
             min_replicas=2,
             target_cpu_utilization_percentage=70).build()
```

## Create an Application

{% code title="application.yaml" %}
```yaml
kind: Application
name: my-app-with-config
....
```
{% endcode %}

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

## Invoke this application

```python
from time import time

import numpy as np
import pandas as pd
from hydrosdk import Cluster, Application

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "my-app-with-config")
predictor = app.predictor()

X = pd.read_csv("training_data.csv")

latencies = []

for sample in X[:100].itertuples(index=False):
    x = np.array(sample)

    t_start = time()
    y = predictor.predict({"x": x})
    t_end = time()
    latencies.append(t_end - t_start)

```

