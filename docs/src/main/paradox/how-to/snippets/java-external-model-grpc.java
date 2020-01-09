import io.hydrosphere.serving.monitoring.MonitoringServiceGrpc;
import io.hydrosphere.serving.monitoring.MonitoringServiceGrpc.MonitoringServiceBlockingStub;
import io.hydrosphere.serving.monitoring.Metadata.ExecutionMetadata;
import io.hydrosphere.serving.monitoring.Api.ExecutionInformation;
import io.hydrosphere.serving.tensorflow.api.Predict.PredictRequest;
import io.hydrosphere.serving.tensorflow.api.Predict.PredictResponse;
import io.hydrosphere.serving.tensorflow.TensorProto;
import io.hydrosphere.serving.tensorflow.TensorShapeProto;
import io.hydrosphere.serving.tensorflow.DataType;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class HydrosphereClient {

    private final String modelName;         // Actual model name, registered within Hydrosphere platform
    private final long modelVersion;        // Model version of the registered model within Hydrosphere platform
    private final long modelVersionId;      // Model version Id, which uniquely identifies any model within Hydrosphere platform
    private final ManagedChannel channel;
    private final MonitoringServiceBlockingStub blockingStub;

    public HydrosphereClient(String target, String modelName, long modelVersion, long modelVersionId) {
        this(ManagedChannelBuilder.forTarget(target).build(), modelName, modelVersion, modelVersionId);
    }

    HydrosphereClient(ManagedChannel channel, String modelName, long modelVersion, long modelVersionId) {
        this.channel = channel;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.modelVersionId = modelVersionId;
        this.blockingStub = MonitoringServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private double getLatency() {
        /*
        Random value is used as an example. Acquire the actual latency
        value, during which a model processed a request.
        */
        return new Random().nextDouble();
    }

    private String getTraceId() {
        /*
        UUID used as an example. Use this value to track down your
        requests within Hydrosphere platform.
        */
        return UUID.randomUUID().toString();
    }

    private TensorProto generateDoubleTensorProto() {
        /*
        Helper method generating random TensorProto object for double values.
        */
        return TensorProto.newBuilder()
                .addDoubleVal(new Random().nextDouble())
                .setDtype(DataType.DT_DOUBLE)
                .setTensorShape(TensorShapeProto.newBuilder().build())  // Empty TensorShape indicates scalar shape
                .build();
    }

    private PredictRequest generatePredictRequest() {
        /*
        PredictRequest is used to define the data passed to the model for inference.
        */
        return PredictRequest.newBuilder()
                .putInputs("in", this.generateDoubleTensorProto()).build();
    }

    private PredictResponse generatePredictResponse() {
        /*
        PredictResponse is used to define the outputs of the model inference.
        */
        return PredictResponse.newBuilder()
                .putOutputs("out", this.generateDoubleTensorProto()).build();
    }

    private ExecutionMetadata generateExecutionMetadata() {
        /*
        ExecutionMetadata is used to define, which model, registered within Hydrosphere
        platform, was used to process a given request.
        */
        return ExecutionMetadata.newBuilder()
                .setModelName(this.modelName)
                .setModelVersion(this.modelVersion)
                .setModelVersionId(this.modelVersionId)
                .setSignatureName("predict")                // Use default signature of the model
                .setLatency(this.getLatency())              // Get latency for a given request
                .setRequestId(this.getTraceId())            // Get traceId to track a given request within Hydrosphere platform
                .build();
    }

    public ExecutionInformation generateExecutionInformation() {
        /*
        ExecutionInformation contains all request data and all auxiliary information
        about request execution, required to calculate metrics.
        */
        return ExecutionInformation.newBuilder()
                .setRequest(this.generatePredictRequest())
                .setResponse(this.generatePredictResponse())
                .setMetadata(this.generateExecutionMetadata())
                .build();
    }

    public void analyzeExecution(ExecutionInformation executionInformation) {
        /*
        The actual use of RPC method Analyse of the MonitoringService to invoke
        metrics calculation.
        */
        this.blockingStub.analyze(executionInformation);
    }

    public static void main(String[] args) throws Exception {
        /*
        Test client functionality by sending randomly generated data for analysis.
        */
        HydrosphereClient client = new HydrosphereClient("dev.k8s.hydrosphere.io", "external-model-example", 1, 643);
        try {
            int requestAmount = 10;
            System.out.printf("Analysing %d randomly generated samples\n", requestAmount);
            for (int i = 0; i < requestAmount; i++) {
                ExecutionInformation executionInformation = client.generateExecutionInformation();
                client.analyzeExecution(executionInformation);
            }
        } finally {
            System.out.println("Shutting down client");
            client.shutdown();
        }
    }
}


