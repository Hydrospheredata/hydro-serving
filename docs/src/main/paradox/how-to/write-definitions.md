# Write definitions

Resource definitions describe Hydrosphere entities. It could be either your model, application, or deployment configuration. Each definition is defined via `.yaml` file.

@@toc { depth=1 }

## Base definition

Every definition **must** include the following fields:

- `kind`: Defines the kind of a resource; 
- `name`: Defines the name of a resource.

The only _valid_ options for `kind` are:

- Model;
- Application.

## kind: Model

The model definition **must** contain the following fields:

- `runtime`: A string, defining a runtime will be used to run a model;
- `contract`: An object, defining the inputs and outputs of a model.

The model definition **can** contain the following fields:

- `payload`: A list of files that should be included into a container;
- `install-command`: A string, defining a command which should be executed during container build;
- `training-data`: A string, defining a path to the file which will be uploaded to the Hydrosphere and used as training data reference. Can be either a local file or a URI to S3 object. At the moment we only support `.csv` files;
- `metadata`: An object, defining additional user metadata which will be displayed on the Hydrosphere UI.

An example below shows, how a model can be defined on a top level.

@@@ vars
```yaml
kind: "Model"
name: "sample_model"
training-data: "s3://bucket/train.csv" | "/temp/file.csv"
runtime: "hydrosphere/serving-runtime-python-3.6:$project.released_version$"
install-command: "sudo apt install jq && pip install -r requirements.txt" 
payload: 
  - "./requirements.txt"
contract:
  ...
metadata:
  ...
```
@@@

### Contract object

`contract` object **must** contain the following fields:

- `inputs`: An object, defining all inputs of a model;
- `outputs`: An object, defining all outputs of a model.

`contract` object **can** contain the following fields:

- `name`: A string, defining a signature of the model, which should be used to calculate request.

#### Field object

`field` object **must** contain the following fields:

- `shape`: Either a string or a list of integers, defining the shape of your data. If shape is defined as a list of integers it can have `-1` value at the very beginning of the list indicating that this field has an arbitrary number of "entities". `-1` cannot be put anywhere aside from the beginning of the list. If shape is defines as a string, it can only be "scalar". 
- `type`: A string, defining the type of your data.

`field` object **can** contain the following fields:

- `profile`: A string, defining the profile type of your data. 

The only _valid_ options for `type` are:

- bool — Boolean;
- string — String in bytes;
- half — 16-bit half-precision floating-point; 
- float16 — 16-bit half-precision floating-point;
- float32 — 32-bit single-precision floating-point;
- double — 64-bit double-precision floating-point;
- float64 — 64-bit double-precision floating-point;
- uint8 — 8-bit unsigned integer;
- uint16 — 16-bit unsigned integer;
- uint32 — 32-bit unsigned integer;
- uint64 — 64-bit unsigned integer;
- int8 — 8-bit signed integer;
- int16 — 16-bit signed integer;
- int32 — 32-bit signed integer;
- int64 — 64-bit signed integer;
- qint8 — Quantized 8-bit signed integer;
- quint8 — Quantized 8-bit unsigned integer;
- qint16 — Quantized 16-bit signed integer;
- quint16 — Quantized 16-bit unsigned integer;
- complex64 — 64-bit single-precision complex;
- complex128 — 128-bit double-precision complex;

The only _valid_ options for `profile` are: 

- text — Monitoring such fields will be done with **text**-oriented algorithms; 
- image — Monitoring such fields will be done with **image**-oriented algorithms; 
- numerical — Monitoring such fields will be done with **numerical**-oriented algorithms.

An example below shows, how a contract can be defined on a top level.

```yaml
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
```

### Metadata object

`metadata` object can represent any arbitrary information, specified by user. The structure of the object is not strictly defined, the only constraint is that the object must have a key-value structure, where a value can only be of a simple data type (string, number, boolean).

An example below shows, how metadata can be defined.

```yaml
metadata:
  experiment: "demo"
  environment: "kubernetes"
```

An example below shows a complete definition of a sample model.

```yaml
kind: "Model"
name: "sample_model"
training-data: "s3://bucket/train.csv" | "/temp/file.csv"
runtime: "hydrosphere/serving-runtime-python-3.6:$project.released_version$"
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

- `singular`: An object, defining a single-model application;
- `pipeline`: A list of objects, defining an application as a pipeline of models. 

### Singular object

`singular` object represents an application, consisting only of one model. The object **must** contain the following fields:

- `model`: A string, defining a model version. It expected to be in the form `model-name:model-version`. 

An example below shows, how a singular application can be defined.

```yaml
kind: "Application"
name: "sample_application"
singular:
  model: "sample_model:1"
```

### Pipeline object

`pipeline` represents a list stages, representing models. 

`stage` object **must** contain the following fields: 

- `model`: A string, defining a model version. It expected to be in the form `model-name:model-version`. 

`stage` object **can** contain the following fields:

- `weight`: A number, defining the weight of the model. All models' weights in a stage must sum up to 100. 

An example below shows, how a pipeline application can be defined.

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

In this application 100% of the traffic will be forwarded to the `claims-preprocessing:1` model version and the output will be fed into `claims-model`. 80% of the traffic will go to the `claims-model:1` model version, 20% of the traffic will go to the `claims-model:2` model version. 