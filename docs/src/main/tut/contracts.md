---
layout: docs
title:  "Contracts"
permalink: 'contracts.html'
---

# Contracts

To let ML Lambda understand your models you should use __contracts__. Contract is a concept that allows you to describe a model itself, it's inputs and outputs. When it's possible ML Lambda can interpretate model without you providing any additional information, although not all frameworks allow that (some of them do not provide required metadata). In that case you would have to describe model by yourself (i.e. write a `contract.prototxt` and `serving.yaml` for the model).

## Frameworks Support

| Framework | Status | Inferring | Commentaries |
| ------- | ------ | --------- | ------------ |
| TensorFlow | `maintained` | 100% | TensorFlow saves all needed metadata in `SavedModel`, so generated contracts will be very accurate.  |
| Spark | `partly` | 50% | Spark has metadata, but it's insufficient and contract inference may be inaccurate. To give an example: 1) there isn't enough notation on how shape of the model is formed (i.e. [30, 40] might be a matrix 30x40 or 40x30); 2) types are not always coincide with what ML Lambda knows, etc. |
| MXNet | `mannual` | 0% | MXNet has it's own export mechanism, but it does not contain any metadata related to types and shapes. |
| SkLearn | `mannual` | 0% | Exported models does not provide required metadata. |
| Theano | `mannual` | 0% | Exported models does not provide required metadata. |
| ONNX | `on hold` | 80% | Currently ML Lambda is able to read ONNX's proto files, but due to the lack of support from other frameworks (PyTorch, TensorFlow, etc.) ONNX models cannot be run in the implemented runtimes. | 

<p style="font-size:0.8em; margin-top: 10px; margin-left: 8px;">
	<code>maintained</code> - No need to provide any self-written contracts, model definitions.<br>
	<code>partly</code> - Complicated models will likely fail inference.<br>
	<code>mannual</code> - Need to provide self-written contracts, model definitions.<br>
	<code>on hold</code> - Full support is postponed.
</p>

## Writing Contracts

If you need to describe the model by yourself, you have to provide 2 files:

### service.yaml

– provides the definition of the model.

```yaml
model:
  name: "linear_regression"
  type: "keras:2.2.0"
  contract: "contract.prototxt"
  payload:
	- "src/" 
	- "model.h5"
```

| Field | Definition |
| ----- | ---------- |
| `name` | Specifies the name of the model. |
| `type` | Specifies the type and the version of the model, e.g. `python:3.6.5`, `tensorflow:1.8.0`, `scikit-learn:0.19.1`, etc. |
| `contract` | Filename of the contract of the model. ML Lambda will look up this file to infer the contract. |
| `payload` | List of all files, used by model. If model is organized with directires/subdirectories, it's enough to only specify root directory. ML Lambda will recoursively add every file/subdirectory inside it. |

<br>

### contract.prototxt

– provides information about your signatures. It closely resembles our defined [ModelSignature][github-model-signature].

```
signatures {
    signature_name: "serving"
	inputs {
	    name: "profile"
		shape: {
		    dim: {
			    size: 3
			}
		}
		dtype: DT_DOUBLE
	}
    inputs {
	    name: "profile_context"
		shape: {
		    dim: {
			    size: 4
			}
			dim: {
			    size: 12
			}
		}
		dtype: DT_DOUBLE
	}
    outputs {
	    name: "user_class"
		subfields {
		    data {
			    name: "user_name"
				dtype: DT_STRING
			}
		}
	}
    outputs {
	    name: "obvserver_class"
		dtype: DT_DOUBLE
	}
    outputs {
	    name: "misc"
		shape: {
		    dim: {
			    size: -1
			}
		}
		dtype: DT_DOUBLE
	}
}
```

| Field | Links | Explanation |
| ----- | --------- | ---------- |
| `inputs`| [ModelFields][github-model-field] | The field is a mapping, which will be provided to the model. The key of the mapping would be the `name` of the field, the value would be actual data.<br>If you want to provide a dictionary with multiple key inputs, simply define another field `inputs` in the signature definition. |
| `outputs`| [ModelFields][github-model-field] | The field is a mapping, which will be expected from the model. The key of the mapping would be the `name` of the field, the value would be actual data.<br>If you want to get a dictionary with multiple key outputs, simply define additional `outputs` in the signature definition. |
| `shape` | [TensorShape][github-tensor-shape] | Describes the shape of the tensor. If you want to specify multidimensional shape, you need to provide multiple `dim` definitions. If you don't know the size of the shape, you can assign `size` to `-1`. ML Lambda will expect 0, 1 or many objects in that case.|
| `dtype` | [DataType][github-datatype] | Can be one of the following values:<br>`DT_INT8`, `DT_INT16`, `DT_INT32`, `DT_INT64`;<br>`DT_UINT8`, `DT_UINT16`, `DT_UINT32`, `DT_UINT64`;<br>`DT_QINT8`, `DT_QINT16`, `DT_QINT32`;<br>`DT_QUINT8`, `DT_QUINT16`;<br>`DT_HALF`, `DT_FLOAT`, `DT_DOUBLE`;<br>`DT_COMPLEX64`, `DT_COMPLEX128`;<br>`DT_BOOL`, `DT_STRING`;<br>`DT_MAP` |

[github-model-signature]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_signature.proto
[github-model-field]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_field.proto
[github-tensor-shape]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/tensor_shape.proto
[github-datatype]: https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/tf/types.proto
