## Model contracts

Model contract is a concept that allows us to describe inputs and outputs of a model.

### ModelField
To describe a single input or output we use 
[ModelField](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_field.proto) 
protobuf message.
We assume that field can describe not only a tensor, but also a complex structure.


## ModelSignature
Several `ModelField` are grouped into [ModelSignature](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_signature.proto)

`ModelSignature` distinguishes field as inputs and outputs.

### ModelContract
[ModelContract](https://github.com/Hydrospheredata/hydro-serving-protos/blob/master/src/hydro_serving_grpc/contract/model_contract.proto)
 is just a set of signatures.
  
### Library support

ML Lambda tries to infer model contract where available.

 - TensorFlow saves all needed metadata in SavedModel, so inference is pretty accurate
 - Spark has a metadata, but it's insufficient and contract inference maybe not accurate.
 - Libraries with pickle-based model export (e.g. sk-learn, theano) do not provide any metadata, 
so ML Lambda can't infer contract for them.
 - Mxnet has it's own model export mechanism, but it does not contain any metadata related to types and shapes.


All protobuf messages are defined in
[hydro-serving-protos](https://github.com/Hydrospheredata/hydro-serving-protos) repository.