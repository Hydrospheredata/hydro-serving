# Monitor anomalies with a custom KNN metric

On this page you will learn how to create a custom anomaly detection metric for a specific use case.

## Overview

For this use case we have chosen a sample classification problem. We will monitor the model, which will classify whether the income of a given person exceeds $50.000 per year. As a data source we will use the census income [dataset](https://archive.ics.uci.edu/ml/datasets/census+income).

## Before you start

We assume you already have a @ref[deployed](../installation/) instance of the Hydrosphere platform and a @ref[CLI](../installation/cli.md) on your local machine.

To let `hs` know where the Hydrosphere platform runs, configure a new `cluster` entity.

```bash
$ hs cluster add --name local --server http://localhost
$ hs cluster use local
```

Also you should have a target classification model already be deployed on the cluster. You can find the source code of the target model [here](https://github.com/Hydrospheredata/hydro-serving-example/tree/master/examples/adult).

## Model training

As a monitoring metric, we will use the KNN outlier detection algorithm from [pyod](https://github.com/yzhao062/pyod) package. Each incoming sample will be scored against predefined clusters, and the final value will be exposed as a monitoring value.

We will skip most of the data preparation steps, for the sake of simplicity.

Python : @@snip[train.py](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/tutorials/monitoring/snippets/knn_anomaly_detection/train.py) { \#main-section }

## Model evaluation

To check that our model works properly, lets plot histograms for training and testing datasets.

Python : @@snip[train.py](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/tutorials/monitoring/snippets/knn_anomaly_detection/train.py) { \#plot-section }

![](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/tutorials/monitoring/images/knn_comparison.png)

## Model release

To create a monitoring metric, we have to deploy that KNN model as a separate model on the Hydrosphere platform. Let's save a trained model for serving.

Python : @@snip[train.py](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/tutorials/monitoring/snippets/knn_anomaly_detection/train.py) { \#save-section }

Create a new directory where we will declare the serving function and its definitions.

```bash
$ mkdir -p monitoring_model/src
$ cd monitoring_model
$ touch src/func_main.py
```

Inside the `src/func_main.py` file put the following code:

Python : @@snip[serve.py](https://github.com/Hydrospheredata/hydro-serving/tree/54b7457851ad9de078cd092f083b8492dea6edca/docs/tutorials/monitoring/snippets/knn_anomaly_detection/serve.py)

This model also have to be packed with a model definition.

```yaml
kind: Model
name: "census_monitoring"
payload:
  - "src/"
  - "requirements.txt"
  - "monitoring_model.joblib"
runtime: "hydrosphere/serving-runtime-python-3.6:$released_version$"
install-command: "pip install -r requirements.txt"
contract:
  name: "predict"
  inputs:
    age:
      shape: scalar
      type: int64
      profile: numerical
    workclass:
      shape: scalar
      type: int64
      profile: numerical
    education:
      shape: scalar
      type: int64
      profile: numerical
    marital_status:
      shape: scalar
      type: int64
      profile: numerical
    occupation:
      shape: scalar
      type: int64
      profile: numerical
    relationship:
      shape: scalar
      type: int64
      profile: numerical
    race:
      shape: scalar
      type: int64
      profile: numerical
    sex:
      shape: scalar
      type: int64
      profile: numerical
    capital_gain:
      shape: scalar
      type: int64
      profile: numerical
    capital_loss:
      shape: scalar
      type: int64
      profile: numerical
    hours_per_week:
      shape: scalar
      type: int64
      profile: numerical
    country:
      shape: scalar
      type: int64
      profile: numerical
    classes:
      shape: scalar
      type: int64
      profile: numerical
  outputs:
    value:
      shape: scalar
      type: double
      profile: numerical
```

Inputs of this model are the inputs of the **target** monitored model plus the outputs of that model. As an output for the monitoring model itself we will use the `value` field.

Pay attention to the model's payload. It has the `src` folder that we have just created, `requirements.txt` with all dependencies and a `monitoring_model.joblib` file, e.g. our newly trained serialized KNN model.

`requirements.txt` looks like this:

```text
joblib==0.13.2
numpy==1.16.2
pyod==0.7.4
```

The final directory structure should look like this:

```text
.
├── monitoring_model.joblib
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

From that folder, upload the model to the cluster.

```bash
$ hs upload
```

## Monitoring

Let's create a monitoring metric for our pre-deployed classification model.

### UI

1. From the _Models_ section, select the target model you would like to deploy and select the desired model version;
2. Open the _Monitoring_ tab.
3. At the bottom of the page click the `Add Metric` button;
4. From the opened window click the `Add Metric` button;
   1. Specify the name of the metric;
   2. Choose the monitoring model;
   3. Choose the version of the monitoring model;
   4. Select a comparison operator `Greater`. This means that if you have a metric value greater than a specified threshold, an alarm should be fired;
   5. Set the threshold value. In this case it should be equal to the value of `monitoring_model.threshold_`.
   6. Click the `Add Metric` button.

That's it. Now you have a monitored income classifier deployed in the Hydrosphere platform.

