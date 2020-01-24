import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.hydrosphere.serving.tensorflow.DataType;
import io.hydrosphere.serving.tensorflow.TensorProto;
import io.hydrosphere.serving.tensorflow.TensorShapeProto;
import io.hydrosphere.serving.tensorflow.api.Model;
import io.hydrosphere.serving.tensorflow.api.Predict;
import io.hydrosphere.serving.tensorflow.api.PredictionServiceGrpc;

import java.util.Random;

public class HydrosphereClient {

    private final String modelName;         // Actual model name, registered within Hydrosphere platform
    private final Int64Value modelVersion;  // Model version of the registered model within Hydrosphere platform
    private final ManagedChannel channel;
    private final PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub;

    public HydrosphereClient2(String target, String modelName, long modelVersion) {
        this(ManagedChannelBuilder.forTarget(target).build(), modelName, modelVersion);
    }

    HydrosphereClient2(ManagedChannel channel, String modelName, long modelVersion) {
        this.channel = channel;
        this.modelName = modelName;
        this.modelVersion = Int64Value.newBuilder().setValue(modelVersion).build();
        this.blockingStub = PredictionServiceGrpc.newBlockingStub(this.channel);
    }

    private Model.ModelSpec getModelSpec() {
        /*
        Helper method to generate ModelSpec.
         */
        return Model.ModelSpec.newBuilder()
                .setName(this.modelName)
                .setVersion(this.modelVersion)
                .build();
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

    public Predict.PredictRequest generatePredictRequest() {
        /*
        PredictRequest is used to define the data passed to the model for inference.
        */
        return Predict.PredictRequest.newBuilder()
                .putInputs("in", this.generateDoubleTensorProto())
                .setModelSpec(this.getModelSpec())
                .build();
    }


    public Predict.PredictResponse predict(Predict.PredictRequest request) {
        /*
        The actual use of RPC method Predict of the PredictionService to invoke prediction.
        */
        return this.blockingStub.predict(request);
    }

    public static void main(String[] args) throws Exception {
        HydrosphereClient client = new HydrosphereClient("<host>", "example", 2);
        Predict.PredictRequest request = client.generatePredictRequest();
        Predict.PredictResponse response = client.predict(request);
        System.out.println(response);
    }
}
