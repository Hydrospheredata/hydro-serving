---
description: 'Estimated completion time: 14 min.'
---

# A/B Analysis for a recommendation model

## Prerequisites

## Setup A/B Application

### Prepare model for upload

{% code title="train\_model.py" %}
```python
import sys

import joblib
from lightfm import LightFM
from lightfm.datasets import fetch_movielens

if __name__ == "__main__":
    no_components = int(sys.argv[1])
    print(f"Number of components is set to {no_components}")

    # Load the MovieLens 100k dataset. Only five
    # star ratings are treated as positive.
    data = fetch_movielens(min_rating=5.0)

    # Instantiate and train the model
    model = LightFM(no_components=no_components, loss='warp')
    model.fit(data['train'], epochs=30, num_threads=2)

    # Save the model
    joblib.dump(model, "model.joblib")
```
{% endcode %}

{% code title="func\_main.py" %}
```python
import joblib
import numpy as np
from lightfm import LightFM

# Load model once
model: LightFM = joblib.load("/model/files/model.joblib")

# Get all item ids
item_ids = np.arange(0, 1682)


def get_top_rank_item(user_id):
    # Calculate scores per item id
    y = model.predict(user_ids=[user_id], item_ids=item_ids)

    # Pick top 3
    top_3 = y.argsort()[:-4:-1]

    # Return {'top_1': ..., 'top_2': ..., 'top_3': ...}
    return dict([(f"top_{i + 1}", item_id) for i, item_id in enumerate(top_3)])
```
{% endcode %}

{% code title="serving.yaml" %}
```yaml
kind: Model
name: movie_rec
runtime: hydrosphere/serving-runtime-python-3.7:2.3.2
install-command: pip install -r requirements.txt
payload:
  - src/
  - requirements.txt
  - model.joblib
contract:
  name: get_top_rank_item
  inputs:
    user_id:
      shape: scalar
      type: int64
  outputs:
    top_1:
      shape: scalar
      type: int64
    top_2:
      shape: scalar
      type: int64
    top_3:
      shape: scalar
      type: int64
```
{% endcode %}

### Upload Model A

```bash
python train_model.py 5
hs upload
```

### 

### Upload Model B



```bash
python train_model.py 20
hs upload
```



```text
hs model list
```



### Create an Application

```python
from application import ApplicationBuilder, ExecutionStageBuilder
from hydrosdk import ModelVersion, Cluster, DeploymentConfiguration

cluster = Cluster('http:\\localhost')

model_a = ModelVersion.find(cluster, "movie_rec", 1)
model_b = ModelVersion.find(cluster, "movie_rec", 2)


stage = ExecutionStageBuilder()
stage = stage_builder.with_model_variant(model_version=model_a, weight=50). \ 
                      with_model_variant(model_version=model_b, weight=50). \ 
                      build()
                                                   
app = ApplicationBuilder(cluster, "movie-ab-app").with_stage(stage).build()
```

### Invoking `movie-ab-app` 

```python
import numpy as np
import pandas as pd
from hydrosdk import Cluster, Application
from lightfm.datasets import fetch_movielens

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "movie-ab-app")
predictor = app.predictor()

# Load the MovieLens 100k dataset.
data = fetch_movielens(min_rating=5.0)


for sample in data['val']:
    pass

```

## Analyse production data

### Read Data from parquet

### Visualise differences

