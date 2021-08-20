---
description: 'Estimated completion time: 14 min.'
---

# A/B Analysis for a Recommendation Model

## Overview

In this tutorial, you will learn how to retrospectively compare the behavior of two different models.

By the end of this tutorial you will know how to:

* Set up an A/B application 
* Analyze production data

## Prerequisites

* [Installed Hydrosphere platform](../installation/)
* [Python SDK](../installation/sdk.md#installation)

## Set Up an A/B Application

### Prepare a model for uploading

{% code title="requirements.txt" %}
```text
lightfm==1.15
numpy~=1.18
joblib~=0.15
tqdm~=4.62.0
```
{% endcode %}

Install the dependencies in your local environment.

```text
pip install -r requirements.txt
```

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
runtime: hydrosphere/serving-runtime-python-3.7:3.0.0-alpha.2
install-command: sudo apt install --yes gcc && pip install -r requirements.txt
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
      profile: numerical
  outputs:
    top_1:
      shape: scalar
      type: int64
      profile: numerical
    top_2:
      shape: scalar
      type: int64
      profile: numerical
    top_3:
      shape: scalar
      type: int64
      profile: numerical
```
{% endcode %}

### Upload Model A

We train and upload our model with 5 components as `movie_rec:v1`

```bash
python train_model.py 5
hs apply -f serving.yaml
```

### Upload Model B

Next, we train and upload a new version of our original model with 20 components as `movie_rec:v2`

```bash
python train_model.py 20
hs apply -f serving.yaml
```

We can check that we have multiple versions of our model by running:

```text
hs model list
```

### Create an Application

To create an A/B deployment we need to create an [Application](../../about/concepts.md#applications) with a single execution stage consisting of two model variants. These model variants are our [Model A](a-b-analysis-for-a-recommendation-model.md#upload-model-a) and [Model B](a-b-analysis-for-a-recommendation-model.md#upload-model-b) correspondingly.

The following code will create such an application:

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

app = ApplicationBuilder("movie-ab-app").with_stage(stage).build(cluster)
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

for uid in tqdm(np.random.choice(user_ids, 2000, replace=True)):
    result = predictor.predict({"user_id": uid})
```

