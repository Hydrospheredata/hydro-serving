# Write definitions

[Resource definitions](../overview/concepts.md#resource-definitions) describe Hydrosphere entities. 

An entity could be your model, application, or deployment configuration. Each definition is represented by a `.yaml` file.

## Base definition

Every definition **must** include the following fields:

* `kind`: Defines the type of resource; 
* `name`: Defines the name of a resource.

The only valid options for `kind` are:

* Model
* Application
* DeploymentConfiguration

## kind: Model

The model definition **must** contain the following fields:

* `runtime`: A string defining the runtime docker image that will be used to run a model. You can learn more about runtimes [here](../overview/concepts.md).
* `contract`: An object defining the inputs and outputs of a model.

The model definition **can** contain the following fields:

* `payload`: A list of files that should be added to the container
* `install-command`: A string defining a command that should be executed during the container build
* `training-data`: A string defining a path to the file that will be uploaded to Hydrosphere and used as a training data reference. It can be either a local file or a URI to an S3 object. At the moment we only support `.csv` files
* `metadata`: An object defining additional user metadata that will be displayed on the Hydrosphere UI

The example below shows how a model can be defined on a top level.

{% code title="serving.yaml" %}
```yaml
kind: "Model"
name: "sample_model"
training-data: "s3://bucket/train.csv" | "/temp/file.csv"
runtime: "hydrosphere/serving-runtime-python-3.6:$released_version$"
install-command: "sudo apt install jq && pip install -r requirements.txt" 
payload: 
  - "./requirements.txt"
contract:
  ...
metadata:
  ...
```
{% endcode %}

### Contract object

`contract` object **must** contain the following fields:

* `inputs`: An object, defining all inputs of a model
* `outputs`: An object, defining all outputs of a model

`contract` object **can** contain the following fields:

* `name`: A string defining the signature of the model that should be used to process requests

#### Field object

`field` object **must** contain the following fields:

* `shape`: Either `"scalar"` or a list of integers, defining the shape of your data. If  a shape is defined as a list of integers, it can have `-1` value at the very beginning of the list, indicating that this field has an arbitrary number of "entities". `-1` cannot be put anywhere aside from the beginning of the list. 
* `type`: A string defining the type of data.

`field` object **can** contain the following fields:

* `profile`: A string, defining the profile type of your data. 

The only _valid_ options for `type` are:

* bool — Boolean;
* string — String in bytes;
* half — 16-bit half-precision floating-point; 
* float16 — 16-bit half-precision floating-point;
* float32 — 32-bit single-precision floating-point;
* double — 64-bit double-precision floating-point;
* float64 — 64-bit double-precision floating-point;
* uint8 — 8-bit unsigned integer;
* uint16 — 16-bit unsigned integer;
* uint32 — 32-bit unsigned integer;
* uint64 — 64-bit unsigned integer;
* int8 — 8-bit signed integer;
* int16 — 16-bit signed integer;
* int32 — 32-bit signed integer;
* int64 — 64-bit signed integer;
* qint8 — Quantized 8-bit signed integer;
* quint8 — Quantized 8-bit unsigned integer;
* qint16 — Quantized 16-bit signed integer;
* quint16 — Quantized 16-bit unsigned integer;
* complex64 — 64-bit single-precision complex;
* complex128 — 128-bit double-precision complex;

The only _valid_ options for `profile` are:

* text — Monitoring such fields will be done with **text**-oriented algorithms 
* image — Monitoring such fields will be done with **image**-oriented algorithms 
* numerical — Monitoring such fields will be done with **numerical**-oriented algorithms
* categorical — Monitoring such fields will be done with **categorical**-oriented algorithms.

The example below shows how a contract can be defined on a top level.

```yaml
name: "infer"
inputs:
  input_field_1:
    shape: [-1, 1]
    type: string
    profile: text
  input_field_2:
    shape: [200, 200]
    type: int32
    profile: categorical
outputs: 
  output_field_1:
    shape: scalar
    type: int32 
    profile: numerical
```

### Metadata object

`metadata` object can represent any arbitrary information specified by the user. The structure of the object is not strictly defined. The only constraint is that the object must have a key-value structure, where a value can only be of a simple data type \(string, number, boolean\).

The example below shows, how metadata can be defined.

```yaml
metadata:
  experiment: "demo"
  environment: "kubernetes"
```

The example below shows a complete definition of a sample model.

```yaml
kind: "Model"
name: "sample_model"
training-data: "s3://bucket/train.csv" | "/temp/file.csv"
runtime: "hydrosphere/serving-runtime-python-3.6:$released_version$"
install-command: "sudo apt install jq && pip install -r requirements.txt" 
payload: 
  - "./*"
contract:
  name: "infer"
  inputs:
    input_field_1:
      shape: [-1, 1]
      type: string
      profile: text
    input_field_2:
      shape: [-1, 1]
      type: int32
      profile: numerical
  outputs: 
    output_field_1:
      shape: scalar
      type: int32 
      profile: numerical
metadata:
  experiment: "demo"
  environment: "kubernetes"
```

## kind: Application

The application definition **must** contain **one** of the following fields:

* `singular`: An object, defining a single-model application;
* `pipeline`: A list of objects, defining an application as a pipeline of models. 

### Singular object

`singular` object represents an application consisting only of one model. The object **must** contain the following fields:

* `model`: A string, defining a model version. It is expected to be in the form `model-name:model-version`. 

The example below shows how a singular application can be defined.

```yaml
kind: "Application"
name: "sample_application"
singular:
  model: "sample_model:1"
```

### Pipeline object

`pipeline` represents a list of stages, representing models.

`stage` object **must** contain the following fields:

* `model`: A string defining a model version. It is expected to be in the form `model-name:model-version`. 

`stage` object **can** contain the following fields:

* `weight`: A number defining the weight of the model. All models' weights in a stage must add up to 100. 

The example below shows how a pipeline application can be defined.

```yaml
kind: Application
name: sample-claims-app
pipeline:
  - - model: "claims-preprocessing:1"
  - - model: "claims-model:1"
      weight: 80
    - model: "claims-model:2"
      weight: 20
```

In this application, 100% of the traffic will be forwarded to the `claims-preprocessing:1` model version and the output will be fed into `claims-model`. 80% of the traffic will go to the `claims-model:1` model version, 20% of the traffic will go to the `claims-model:2` model version.

## kind: DeploymentConfiguration

The DeploymentConfiguration resource definition **can** contain the following fields:

* `hpa`: An object defining [HorizontalPodAutoscalerSpec](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#horizontalpodautoscalerspec-v1-autoscaling)
* `container`: An object defining settings applied on a container level
* `deployment`: An object defining settings applied on a deployment level
* `pod`: An object defining settings applied on a pod level

### HPA object

hpa object closely resembles the Kubernetes [HorizontalPodAutoscalerSpec](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#horizontalpodautoscalerspec-v1-autoscaling) object

hpa object **must** contain:

* `minReplicas` : minReplicas is the lower limit for the number of replicas to which the autoscaler can scale down;
* `maxReplicas` : integer, upper limit for the number of pods that can be set by the autoscaler; cannot be smaller than minReplicas;
* `cpuUtilization` : integer from 1 to 100, target average CPU utilization \(represented as a percentage of requested CPU\) over all the pods; if not specified the default autoscaling policy will be used.

### Container object

container object **can** contain:

* `resources` : object with `limits` and `requests` fields. Closely resembles the k8s [ResourceRequirements](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#resourcerequirements-v1-core) object 

### Pod object

hpa object is similar to the Kubernetes [PodSpec](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#podspec-v1-core) object

pod object **can** contain

* `nodeSelector` : [selector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#nodeselector-v1-core) which must be true for the pod to fit on a node. Selector which must match a node's labels for the pod to be scheduled on that node. [More info](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/).
* `affinity` : pod's scheduling constraints. Represented by an [Affinity](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#affinity-v1-core) object
* `tolerations` : Array of [Tolerations](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#toleration-v1-core)

### Deployment object

deployment object **must** contain:

* `replicaCount` : integer, number of desired pods. This is a pointer to distinguish between explicit zero and not specified. Defaults to 1.

### Example

The example below shows how a deployment configuration can be defined.

```yaml
kind: DeploymentConfiguration
name: cool-deployment-config
hpa:
  minReplicas: 2
  maxReplicas: 10
  cpuUtilization: 80
deployment:
  replicaCount: 4
container:
  resources:
    limits:
      cpu: '4'
      memory: 4g
    requests:
      cpu: '2'
      memory: 2g
pod:
  nodeSelector:
    im: a map
    foo: bar
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: exp1
            operator: Exists
          matchFields:
          - key: fields1
            operator: Exists
      preferredDuringSchedulingIgnoredDuringExecution:
      - preference:
          matchExpressions:
          - key: exp2
            operator: NotIn
            values:
            - aaaa
            - bvzv
            - czxc
          matchFields:
          - key: fields3
            operator: NotIn
            values:
            - aaa
            - cccc
            - zxcc
        weight: 100
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: value
            operator: Exists
          - key: key
            operator: NotIn
            values:
            - a
            - b
        namespaces:
        - namespace1
        topologyKey: top
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchLabels:
              key: a
            matchExpressions:
            - key: key1
              operator: In
              values:
              - a
              - b
            - key: value2
              operator: NotIn
              values:
              - b
          namespaces:
          - namespace2
          topologyKey: topo_valur
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: value
            operator: Exists
          - key: key2
            operator: NotIn
            values:
            - a
            - b
          - key: key3
            operator: DoesNotExist
        namespaces:
        - namespace1
        topologyKey: top
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchLabels:
              key: a
            matchExpressions:
            - key: key
              operator: In
              values:
              - a
              - b
            - key: key2
              operator: NotIn
              values:
              - b
          namespaces:
          - namespace2
          topologyKey: toptop
  tolerations:
  - effect: PreferNoSchedule
    key: equalToleration
    tolerationSeconds: 30
    operator: Equal
    value: kek
  - key: equalToleration
    operator: Exists
    effect: PreferNoSchedule
    tolerationSeconds: 30
```

