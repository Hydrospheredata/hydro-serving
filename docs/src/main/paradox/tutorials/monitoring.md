
# Monitoring Models

To monitor the model during production load we can use several built-in metrics. There are a few ways to add a metric to the model: 

1. [From the UI](#ui);
1. [In the model resource definition](#resource-definition);
1. [Via SDK](#sdk).

Let's quickly revise those ways step-by-step. The model used in the examples below is available [here](https://github.com/Hydrospheredata/hydro-serving-example/blob/master/examples/adult/model/adult_tensor/serving.yaml). The metrics that we are going to assign are available by default. 

## UI

The most simplest way to add a metric is to open a model in the UI, select a desired version and assign a new metric from the settings window in the monitoring tab. 

## Resource Definition

You can also define required metrics in the resource definition of the model. To do this you have to supply additional __monitoring__ field in the `serving.yaml` file. Let's look at the example:

```yaml
kind: Model
name: "adult_tensor"
runtime: "hydrosphere/serving-runtime-python-3.6:0.1.2-rc0"

payload:
- "src/"
- "../requirements.txt"
- "../random-forest-adult.joblib"

install-command: "pip install -r requirements.txt"

contract:
  name: "predict"
  inputs:
    features:
      shape: [-1, 12]
      type: int64
  outputs:
    classes:
      shape: [-1, 1]
      type: int64

monitoring:
  - name: "Latency"
    kind: LatencyMetricSpec
    "with-health": true
    config:
      interval: 15
      threshold: 10
  - name: "Service Errors"
    kind: ErrorRateMetricSpec
    "with-health": true
    config:
      interval: 15
      threshold: 3
  - name: "Counter"
    kind: CounterMetricSpec
    config: 
      interval: 15
```

Here we assigned four different metrics to our model. Once the resource definition of the model is changed, the next model upload will be supplied with additional metrics.

## SDK

You can also add monitoring to a model using Python SDK library. This library can be used within your automation pipeline to continuously deliver machine learning models to production.

First, let's install SDK.

```sh
pip install hydrosdk
```

Next, we are going to declare actual model definitions and the deployment. 

```python
from hydrosdk import sdk

# First we define monitoring metrics and all other necessary attributes 
monitoring = [
    (
        sdk.Monitoring('Latency')
        .with_health()
        .with_spec(
            'LatencyMetricSpec', 
            interval=15, 
            threshold=10
        ),
    ),
    (
        sdk.Monitoring('Service Errors')
        .with_health()
        .with_spec(
            'ErrorRateMetricSpec',
            interval=15, 
            threshold=3
        )
    ),
    (
        sdk.Monitoring('Counter')
        .with_spec(
            'CounterMetricSpec',
            interval=15
        )
    )
]

name = ...
runtime = ...
payload = ...
...

# Next, we assemble the model 
model = (
    sdk.Model()
    .with_name(name)
    .with_runtime(runtime)
    .with_payload(payload)
    ...
    .with_monitoring(monitoring)
)

# Finally, we upload the model to the cluster
model.apply(platform_uri)
```

After executing this script the model will be assembled and uploaded to the platform just as with regular `serving.yaml`. 

@@@ note
To learn more about SDK, refer to [this page](../components/sdk.md). 
@@@

@@@ note
To learn more about different monitoring metrics, refer to [this page](../components/sdk.md). 
@@@