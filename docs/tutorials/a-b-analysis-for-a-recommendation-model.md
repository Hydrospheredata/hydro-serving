---
description: 'Estimated completion time: 14 min.'
---

# A/B Analysis for a recommendation model

In this tutorial we'll show how users can retrospectively compare behaviour of two different models.

## Prerequisites

* [Installed Hydrosphere platform](../installation/)
* [Python SDK](../installation/sdk.md#installation)

## Setup A/B Application

### Prepare model for upload

{% code title="requirements.txt" %}
```text
lightfm==1.15
numpy~=1.18
joblib~=0.15
```
{% endcode %}

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

{% code title="src/func\_main.py" %}
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

We train and upload our model with 5 components as `movie_rec:v1`

```bash
python train_model.py 5
hs upload
```

### Upload Model B

Next, we train and upload new version of our original model with 20 components as `movie_rec:v2`

```bash
python train_model.py 20
hs upload
```

We can check that we have multiple versions of our model by running

```text
hs model list
```

### Create an Application

To create an A/B deployment we need to create an [Application](../overview/concepts.md#applications) with single execution stage consisting of two model variants. These model variants are our  [Model A](a-b-analysis-for-a-recommendation-model.md#upload-model-a) and [Model B](a-b-analysis-for-a-recommendation-model.md#upload-model-b) correspondingly.

The following code will create such application

```python
from hydrosdk import ModelVersion, Cluster
from hydrosdk.application import ApplicationBuilder, ExecutionStageBuilder

cluster = Cluster('http://localhost')

model_a = ModelVersion.find(cluster, "movie_rec", 1)
model_b = ModelVersion.find(cluster, "movie_rec", 2)

stage_builder = ExecutionStageBuilder()
stage = stage_builder.with_model_variant(model_version=model_a, weight=50). \
    with_model_variant(model_version=model_b, weight=50). \
    build()

app = ApplicationBuilder(cluster, "movie-ab-app").with_stage(stage).build()
```

### Invoking `movie-ab-app` 

We'll simulate production data flow by repeatedly asking our model for recommendations.

```python
import numpy as np
from hydrosdk import Cluster, Application
from tqdm.auto import tqdm

cluster = Cluster("http://localhost", grpc_address="localhost:9090")

app = Application.find(cluster, "movie-ab-app")
predictor = app.predictor()

user_ids = np.arange(0, 943)

simulated_prod_requests = np.random.choice(user_ids, 2000, replace=True)
for uid in tqdm(simulated_prod_requests):
    result = predictor.predict({"user_id": uid})
```

## Analyse production data

### Read Data from parquet

### Visualise differences

