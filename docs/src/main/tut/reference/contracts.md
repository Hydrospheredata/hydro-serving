## Contracts

If your models is not supported or inferrence percentage is low, you may need to describe the model by yourself. To do that you should use __contracts__. Contract is a concept that allows you to decsribe model's inputs and outputs. 

```yaml
kind: Model
name: "example_tokenizer_model"
model-type: "python:3.6"
payload:
  - "src/"
  - "requirements.txt/"
  - "nltk_data"
  
contract:
  tokenize:                 # arbitrary: name of the signature
    inputs:                 
      input_text:           # arbitrary: name of the field
        shape: [-1]         # 1D array with arbitrary shape
        type: string
        profile: text
    outputs:                
      output_text:          # arbitrary: name of the field
        shape: [-1]
        type: string
        profile: text
```

`model-type` specifies the type and the version of the model, e.g. `python:3.6.5`, `tensorflow:1.8.0`, `scikit-learn:0.19.1`, etc. It should has the format `<framework>:<version>`. 

`contract` might consist of multiple signatures with different names. The choice of the name depends only on you. Later on in the web interface you can choose between defined signatures in your applications. `inputs` and `outputs` share the same annotation. 

| Field | Definition | Type | Type description |
| ----- | ---------- | ---- | ---------------- |
| `shape` | Describes the shape of the input/ouput tensor. | `scalar` | Single number |
| | | `-1` | Arbitrary shape | 
| | | `1`, `2`, `3`, ... | Any positive number |

<br>

| Field | Definition | Type | Type description |
| ----- | ---------- | ---- | ---------------- |
| `type` |  Describes the data type of incoming tensor. | `bool` | Boolean | 
| | | `string`      | String | 
| | | `half`, `float16` | 16-bit half-precision floating-point |
| | | `float32`     | 32-bit single-precision floating-point | 
| | | `double`, `float64`      | 64-bit double-precision floating-point | 
| | | `uint8`       | 8-bit unsigned integer | 
| | | `uint16`      | 16-bit unsigned integer | 
| | | `uint32`      | 32-bit unsigned integer | 
| | | `uint64`      | 64-bit unsigned integer | 
| | | `int8`        | 8-bit signed integer | 
| | | `int16`       | 16-bit signed integer | 
| | | `int32`       | 32-bit signed integer | 
| | | `int64`       | 64-bit signed integer | 
| | | `qint8`       | Quantized 8-bit signed integer | 
| | | `quint8`      | Quantized 8-bit unsigned integer | 
| | | `qint16`      | Quantized 16-bit signed integer | 
| | | `quint16`     | Quantized 16-bit unsigned integer |
| | | `complex64`   | 64-bit single-precision complex | 
| | | `complex128`  | 128-bit double-precision complex | 

<br>

| Field | Definition | Type | Type description |
| ----- | ---------- | ---- | ---------------- |
| `profile` | Describes the nature of the data. | `text` | Monitoring such fields will be done with __text__-oriented algorithms. |
| | | `image` | Monitoring such fields will be done with __image__-oriented algorithms. | 
| | | `numerical` | Monitoring such fields will be done with __numerical__-oriented algorithms.|
