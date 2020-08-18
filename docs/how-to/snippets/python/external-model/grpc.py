import uuid
import grpc
import random
import hydro_serving_grpc as hs

use_ssl_connection = True
if use_ssl_connection:
    creds = grpc.ssl_channel_credentials()
    channel = grpc.secure_channel(HYDROSPHERE_INSTANCE_GRPC_URI, credentials=creds)
else:
    channel = grpc.insecure_channel(HYDROSPHERE_INSTANCE_GRPC_URI) 
monitoring_stub = hs.MonitoringServiceStub(channel)

# 1. Create an ExecutionMetadata message. ExecutionMetadata is used to define, 
# which model, registered within Hydrosphere platform, was used to process a 
# given request.
trace_id = str(uuid.uuid4())  # uuid used as an example
execution_metadata_proto = hs.ExecutionMetadata(
    model_name="external-model-example",
    modelVersion_id=2,
    model_version=3,
    signature_name="predict",
    request_id=trace_id,
    latency=0.014,
)

# 2. Create a PredictRequest message. PredictRequest is used to define the data 
# passed to the model for inference.
predict_request_proto = hs.PredictRequest(
    model_spec=hs.ModelSpec(
        name="external-model-example",
        signature_name="predict", 
    ),
    inputs={
        "in": hs.TensorProto(
            dtype=hs.DT_DOUBLE, 
            double_val=[random.random()], 
            tensor_shape=hs.TensorShapeProto()
        ),
    }, 
)

# 3. Create a PredictResponse message. PredictResponse is used to define the 
# outputs of the model inference.
predict_response_proto = hs.PredictResponse(
    outputs={
        "out": hs.TensorProto(
            dtype=hs.DT_DOUBLE, 
            double_val=[random.random()], 
            tensor_shape=hs.TensorShapeProto()
        ),
    },
)

# 4. Create an ExecutionInformation message. ExecutionInformation contains all 
# request data and all auxiliary information about request execution, required 
# to calculate metrics.
execution_information_proto = hs.ExecutionInformation(
    request=predict_request_proto,
    response=predict_response_proto,
    metadata=execution_metadata_proto,
)

# 5. Use RPC method Analyse of the MonitoringService to calculate metrics
monitoring_stub.Analyze(execution_information_proto)
