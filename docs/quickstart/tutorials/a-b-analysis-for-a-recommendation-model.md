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

{% code title="setup\_runtime.sh" %}
```bash
apt install --yes gcc
pip install -r requirements.txt
```
{% endcode %}

{% code title="serving.yaml" %}
```yaml
kind: Model
name: movie_rec
runtime: hydrosphere/serving-runtime-python-3.7:2.3.2
install-command: chmod a+x setup_runtime.sh && ./setup_runtime.sh
payload:
  - src/
  - requirements.txt
  - model.joblib
  - setup_runtime.sh
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

Next, we train and upload a new version of our original model with 20 components as `movie_rec:v2`

```bash
python train_model.py 20
hs upload
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

for uid in tqdm(np.random.choice(user_ids, 2000, replace=True)):
    result = predictor.predict({"user_id": uid})
```

## Analyze production data

### Read Data from parquet

Each request-response pair is stored in S3 \(or in minio if deployed locally\) in parquet files. We'll use `fastparquet` package to read these files and use `s3fs` package to connect to S3.

```python
import fastparquet as fp
import s3fs

s3 = s3fs.S3FileSystem(client_kwargs={'endpoint_url': 'http://localhost:9000'},
                       key='minio', secret='minio123')

# The data is stored in `feature-lake` bucket by default 
# Lets print files in this folder
s3.ls("feature-lake/")
```

The only file in the `feature-lake` folder is `['feature-lake/movie_rec']`. Data stored in S3 is stored under the following path: `feature-lake/MODEL_NAME/MODEL_VERSION/YEAR/MONTH/DAY/*.parquet`

```python
# We fetch all parquet files with glob
version_1_paths = s3.glob("feature-lake/movie_rec/1/*/*/*/*.parquet")
version_2_paths = s3.glob("feature-lake/movie_rec/2/*/*/*/*.parquet")

myopen = s3.open

# use s3fs as the filesystem to read parquet files into a pandas dataframe
fp_obj = fp.ParquetFile(version_1_paths, open_with=myopen)
df_1 = fp_obj.to_pandas()

fp_obj = fp.ParquetFile(version_2_paths, open_with=myopen)
df_2 = fp_obj.to_pandas()
```

Now that we have loaded the data, we can start analyzing it.

### Compare production data with new labeled data

To compare differences between model versions we'll use two metrics:

1. Latency - we compare the time delay between the request received and the response produced.
2. Mean Top-3 Hit Rate - we compare recommendations to those the user has rated. If they match then increase the hit rate by 1. Do this for the complete test set to get the hit rate.

#### Latencies

Let's calculate the 95th percentile of our latency distributions per model version and plot them. Latencies are stored in the `_hs_latency` column in our dataframes.

```python
latency_v1 = df_1._hs_latency
latency_v2 = df_2._hs_latency

p95_v1 =  latency_v1.quantile(0.95)
p95_v2 = latency_v2.quantile(0.95)
```

In our case, the output was 13.0ms against 12.0ms. Results may differ.

Furthermore, we can visualize our data. To plot latency distribution we'll use the Matplotlib library.

```python
import matplotlib.pyplot as plt

# Resize the canvas
plt.gcf().set_size_inches(10, 5)

# Plot latency histograms
plt.hist(latency_v1, range=(0, 20),
 normed=True, bins=20, alpha=0.6, label="Latency Model v1")
plt.hist(latency_v2, range=(0, 20),
 normed=True, bins=20, alpha=0.6, label="Latency Model v2")

# Plot previously computed percentiles
plt.vlines(p95_v1, 0, 0.1, color="#1f77b4",
 label="95th percentile for model version 1")
plt.vlines(p95_v2, 0, 0.1, color="#ff7f0e",
 label="95th percentile for model version 2")

plt.legend()
plt.title("Latency Comparison between v1 and v2")
```

![](../../.gitbook/assets/image%20%281%29%20%281%29%20%284%29%20%286%29%20%281%29.png)

#### Mean Top-3 Hit Rate

Next, we'll calculate hit rates. To do so, we need new labeled data. For recommender systems, this data is usually available after a user has clicked\watched\liked\rated the item we've recommended to him. We'll use the test part of movielens as labeled data.

To measure how well our models were recommending movies we'll use a hit rate metric. It calculates how many movies users have watched and rated with 4 or 5 out of 3 movies recommended to him.

```python
from lightfm.datasets import fetch_movielens

test_data = fetch_movielens(min_rating=5.0)['test']
test_data = test_data.toarray()

# Dict with model version as key and mean hit rate as value
mean_hit_rate = {}
for version, df in {"v1": df_1, "v2": df_2}.items():

    # Dict with user id as key and hit rate as value
    hit_rates = {}
    for x in df.itertuples():
        hit_rates[x.user_id] = 0

        for top_x in ("top_1", "top_2", "top_3"):
            hit_rates[x.user_id] += test_data[x.user_id, getattr(x, top_x)] >= 4

    mean_hit_rate[version] = round(sum(hit_rates.values()) / len(hit_rates), 3)
```

In our case the `mean_hit_rate` variable is `{'v1': 0.137, 'v2': 0.141}` . Which means that the second model version is better in terms of hit rate.

You have successfully completed the tutorial! 🚀

Now you know how to read and analyze automatically stored data.

