# Write resource definitions

Resource definitions describe serving cluster entities. It could be your 
model, application or deployment configuration. The type of each definition 
is defined by `kind` field. 

```yaml
kind: Model # | Application | HostSelector
name: "example"
```

## Model

### Fields

- `runtime` defines the docker image that will be used in deployment.
- `payload` defines all files for the model.
- `contract` defines a prediction signature of a model.
- `install-command` defines an initialization command to be executed 
during upload procedure.
- `training-data` defines a local file or path to S3 object where training 
data is stored.

### Example

```yaml
kind: Model
name: sample_model
training-data: s3://bucket/train.csv
runtime: hydrosphere/serving-runtime-dummy:dev
install-command: "sudo apt install jq" 
payload: 
  - "./*"
contract:
  name: infer
  inputs:
	input_field_1:
	  shape: [-1, 1]
	  type: string
	  profile: text
	input_field_2:
	  shape: scalar
	  type: int32
	  profile: numerical
  outputs: 
	output_field_1:
	  shape: [-1, 2]
	  type: int32 
	  profile: numerical
metadata:
  experiment: "demo"
  model_version: "1.1"

```

In the example above we've defined a signature with `infer` name. Each 
signature has to have `inputs` and `outputs`. They define what kind of 
data the model will receive and what will it produce. Each input and 
output field has the following defined properties: `shape`, `type` and 
`profile`. 


## Application

For this type of resource you have to declare one of the following fields:

- `singular` defines a single-model application. 
- `pipeline` defines application as a pipeline of models.  

### Singular

`singular` applications usually consist of smaller amount of definitions. 

```yaml
kind: Application
name: sample_application
singular:
  model: sample_model:1
```

`singular` field has a single `model` property. It's expected to be in 
the form `model-name:model-version`.

### Pipeline

`pipeline` applications have more detailed definitions.

```yaml
kind: Application
name: sample-claims-app
pipeline:
  - model: claims-preprocessing:1
    modelservices:
      - model: claims-model:1
        weight: 80
      - model: claims-model-old:2
        weight: 20
```

`pipeline` is a list of stages. Each item in the list can have the 
following attributes:

- `model` defines the model and its version to use. Expected to be in the 
form `model-name:model-version`.
- A stage can consist of multiple models. In that case you can define 
`modelservices` where you will list needed models. For each model in you 
would have to declare a `weight` attribute, which has to sum up to 100 
across all the models in the stage. The `weight` defines how much traffic 
would go through the model.


## HostSelector

HostSelector (or deployment configuration) gives an ability to set 
environment requirements for your model deployment. Your model uses GPU? 
Maybe you want some experimental ARM64 version? Other requirements? 
This resource is for you.

Having said that, it's not fully implemented since it depends on cluster 
infrastructure and cloud provider. Will be available in the upcoming 
releases.
