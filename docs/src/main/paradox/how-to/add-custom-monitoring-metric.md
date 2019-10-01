# Add custom monitoring metric

Dozens of new models are introduced every month and monitoring all them 
is extremely hard due to variability of types and architectures that lie 
upon them. To mitigate this issue we introduced the ability to write 
custom monitoring metrics. In this article you will learn how to create 
one with in depth explanations. 

## Case

As an example we chose to create a custom metric, which will track the 
reconstruction rate of your production data. It will let you know how 
similar your production data to your training data. 

### Monitored Model

The actual model that we want to monitor tries to predict based on a set 
of features, will the person earn more than $50k per year or not. For the 
sake of example, we are defining only a subset of features. 

```yaml
kind: Model
name: adult-salary
payload:
  - "src/"
  - "requirements.txt"
  - "classification_model.joblib"
runtime: "hydrosphere/serving-runtime-python-3.6:0.1.2-rc0"
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
  outputs:
    classes:
      shape: scalar
      type: int64
      profile: numerical
```

As an output the model produces either 0 or 1.

## Custom Metric 

In order to calculate a custom metric you have to provide a code that will
do the calculations. To do this you have to write another model which will 
get messages via gRPC protocol just as a regular model does. 

### Inference code 

```python
import hydro_serving_grpc as hs
import numpy as np
from joblib import load
from utils import extract_value

features = ['age', 'workclass', 'education']
monitoring_model = load('/model/files/monitoring_model.joblib')

def predict(**kwargs):
    extracted = np.array(
        [extract_value(kwargs[feature]) for feature in features])
    transformed = np.dstack(extracted).reshape(1, len(features))
    predicted = monitoring_model.decision_function(transformed)
    
    response = hs.TensorProto(
        double_val=[predicted.item()],
        dtype=hs.DT_DOUBLE,
        tensor_shape=hs.TensorShapeProto()
    )
    return hs.PredictResponse(
        outputs={"value": response})
```

@@@ note
Your monitoring model is restricted to return a scalar DT_DOUBLE value
to the monitoring service. Also, make sure that you are sending back a 
message with `value` key. 
@@@

You may ask yourself what are the inputs to this model? Well, those are 
the inputs plus the outputs of `adult-salary` model, defined above. Why 
do we also need outputs of the monitored models? There are cases, when 
you need to know the outputs of the model as well to calculate metrics, 
but if you don't need them right away, just leave them alone. We will 
cover cases with model outputs more deeply in the upcoming releases.

### Metric Resource Definition

Because monitoring model will be served just as a regular model, it also 
needs a defined contract.

```yaml
kind: Model
name: "adult-salary-metric"
payload:
  - "src/"
  - "requirements.txt"
  - "monitoring_model.joblib"
runtime: "hydrosphere/serving-runtime-python-3.6:0.1.2-rc0"
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

### Deployment 

Once this is done, we can upload monitored model to the cluster just as
a regular model.

```sh 
hs upload 
``` 

To use a this metric you have to create an application. You can create one by:

- writing a [resource definition](./write-resource-definitions.html#application) 
for the application and applying it to the cluster. 
- creating an application manually from UI.

We suppose you are already know how to do that, so we move onto the next 
part: assigning metric to the monitored model. 

## Assigning Metric

There are a few ways to add a metric to the model: 

1. [From the UI](#ui);
1. [In the model resource definition](#resource-definition);
1. [Via SDK](#sdk).

### UI

The most simplest way to add a metric is to open a model in the UI, select 
a desired version and assign a new metric from the settings window in the 
monitoring tab. You have to specify that this is a `Custom Model` metric, 
choose an application with a monitoring model `adult-salary-metric-app`,  
define the comparison operator and set a threshold. Monitoring will fire, 
when the value, produced by your monitoring model exceeds your defined 
threshold (with respect to comparison operator). 

### Resource Definition

You can also define metrics in the resource definition of the model, which 
you want to monitor. In our case this would be `adult-salary` model. To do 
this you have to supply additional __monitoring__ field in the `serving.yaml` 
file.

```yaml
kind: Model
name: adult-salary
payload:
  - "src/"
  - "requirements.txt"
  - "classification_model.joblib"
runtime: "hydrosphere/serving-runtime-python-3.6:0.1.2-rc0"
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
  outputs:
    classes:
      shape: scalar
      type: int64
      profile: numerical
monitoring:
  - name: Custom Metric
    kind: CustomModelSpec
    "with-health": true
    config:
      application: adult-salary-metric-app
      interval: 15
      threshold: 10
      comparison-operator: ">="
```

Once the resource definition of the model is changed, the next model 
upload will be supplied with additional metrics.

### SDK

You can also add monitoring to a model using Python SDK library. This 
library can be used within your automation pipeline to continuously 
deliver machine learning models to production.

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
        sdk.Monitoring('Custom Metric')
        .with_health()
        .with_spec(
            'CustomModelSpec', 
            application="adult-salary-metric-app",
            comparison_operator=">=",
            interval=15, 
            threshold=10,
        ),
    ),
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

After executing this script the model will be assembled and uploaded to 
the platform just as with regular `serving.yaml` gets executed. 

@@@ note
To learn more about SDK, refer to [this page](../components/sdk.md). 
@@@
