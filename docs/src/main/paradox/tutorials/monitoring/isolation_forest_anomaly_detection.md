# Monitor anomalies with a custom Isolation Forest metric

In this page you will learn how to create a custom anomaly detection metric for the specific use case. 

@@toc { depth=1 }

## Overview

For the use case we've picked a sample regression problem. We will monitor the model, which will predict how much taxi pickups will occur in the next hour based on the observations from last 5 hours. As a data source we will use [dataset](https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page) from NYC Taxi & Limousine Commission.

## Before you start

We assume you already have a @ref[deployed](../../install/platform.md) instance of the Hydrosphere platform and a @ref[CLI](../../install/client/cli.md) on your local machine.

To let `hs` know where the Hydrosphere platform runs, configure a new `cluster` entity. 

```sh 
$ hs cluster add --name local --server http://localhost
$ hs cluster use local
```

Also you should have a target regression model already be deployed on the cluster. You can find the source code of the target model here. 


## Model training

As a monitoring model we will use an autoregressive stateful IsolationForest model, which will be continuously retrained on a window of 5 consequent data samples. 

We will skip most of the data preparation steps just for the sake of simplicity. 

Python
:   @@snip[train.py](snippets/isolation_forest_anomaly_detection/train.py) { #main-section }

## Model evaluation

To check that our model works properly, lets plot training data and outliers.

Python
:   @@snip[train.py](snippets/isolation_forest_anomaly_detection/train.py) { #plot-section }

![](.../stateful_isolation_forest_taxi_plot.png)

From the plot you can see massive amount of anomalies in the end of January 2016. These outliers came from travel ban due to ["Snowzilla"](https://en.wikipedia.org/wiki/January_2016_United_States_blizzard).

## Model release

To create a monitoring metric we have to deploy that IsolationForest model as a separate model in Hydrosphere. Let's save a trained model for serving. 

Python
:   @@snip[train.py](snippets/isolation_forest_anomaly_detection/train.py) { #save-section }

Create a new directory where we will declare the serving function and its definitions. 

```sh
$ mkdir -p monitoring_model/src
$ cd monitoring_model
$ touch src/func_main.py
```

Inside the `src/func_main.py` file put the following code:

Python
:   @@snip[serve.py](snippets/isolation_forest_anomaly_detection/serve.py)

This model also have to be packed with a model definition.

@@@ vars
```yaml
kind: Model
name: nyc_taxi_monitoring
runtime: "hydrosphere/serving-runtime-python-3.6:$project.released_version$"
install-command: "pip install -r requirements.txt"
payload:
  - "src/"
  - "requirements.txt"
  - "iforest.joblib"

contract:
  name: infer
  inputs:
    pickups_last_hour:
      shape: scalar
      type: int32
      profile: numerical
    pickups_next_hour:
      shape: scalar
      type: int32
      profile: numerical
  outputs:
    value:
      shape: scalar
      type: double
      profile: numerical
```
@@@

Inputs of this model are the inputs of the monitored model plus the outputs of the monitored model. As an output for the monitoring model we use `value` field. 

Pay attention to the model's payload. It has `src` folder that we've just created, `requirements.txt` with all dependencies and `iforest.joblib`, e.g. our newly trained serialized IsolationForest model. 

`requirements.txt` looks like this: 

```
joblib==0.13.2
numpy==1.16.2
scikit-learn==0.20.2
```

The final directory structure should look like this: 

```
.
├── iforest.joblib
├── requirements.txt
├── serving.yaml
└── src
    └── func_main.py
```

From that folder upload the model to the Hydrosphere.

```sh
hs upload
```

## Monitoring

Let's create a monitoring metric for our pre-deployed regression model. 

### UI

1. From the _Models_ section pick the target model, which you would like to deploy and select the desired model version;
1. Open _Monitoring_ tab.
1. At the bottom of the page click `Add Metric` button;
1. From the opened window click `Add Metric` button;
    1. Specify the name of metric;
    1. Choose monitoring model;
    1. Choose version of the monitoring model;
    1. Pick a comparison operator `GreaterEq`. This means that whenever our metric value drops below 0, an alarm would be fired.
    1. Set the threshold value to 0.
    1. Click `Add Metric` button.

That's it. Now you have a monitored taxi pickups regression model deployed on the Hydrosphere platform. 
