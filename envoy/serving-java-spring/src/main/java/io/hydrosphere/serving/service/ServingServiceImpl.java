package io.hydrosphere.serving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.hydrosphere.serving.proto.Empty;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.proto.ServingServiceGrpc;
import io.hydrosphere.serving.proto.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 *
 */
public class ServingServiceImpl extends ServingServiceGrpc.ServingServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AuthorityReplacerInterceptor.class.getName());

    private final ServingServiceGrpc.ServingServiceStub servingServiceStub;

    private final StageExecutor stageExecutor;

    private final Executor executor;

    private final String currentDestination;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServingServiceImpl(ServingServiceGrpc.ServingServiceStub servingServiceStub,
                              StageExecutor stageExecutor,
                              Executor executor, String currentDestination) {
        this.servingServiceStub = servingServiceStub;
        this.stageExecutor = stageExecutor;
        this.executor = executor;
        this.currentDestination = currentDestination;
    }

    @Override
    public void serve(ServingPipeline request, StreamObserver<Empty> responseObserver) {
        responseObserver.onCompleted();
        logger.info("Received request: {}", request);

        byte[] data = request.getData().toByteArray();
        if (request.getStageSequence() < 0 || request.getStageSequence() >= request.getStagesCount()) {
            sendErrorMessage(request, "Wrong stage sequence");
            return;
        }
        Stage stage = request.getStagesList().get(request.getStageSequence());
        String action = stage.getAction();

        CompletableFuture.supplyAsync(() -> {
            try {
                return stageExecutor.execute(action, objectMapper.readTree(data));
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }, executor)
                .whenComplete((res, ex) -> {
                    if (ex == null) {
                        int nextSeq = request.getStageSequence() + 1;
                        ServingPipeline next;
                        try {
                            next = request.toBuilder()
                                    .setData(ByteString.copyFrom(objectMapper.writeValueAsBytes(res)))
                                    .setStageSequence(request.getStageSequence() + 1)
                                    .build();
                        } catch (JsonProcessingException e) {
                            logger.error(e.getMessage(), e);
                            sendErrorMessage(request, e.getMessage());
                            return;
                        }
                        String destination;
                        if (nextSeq >= request.getStagesCount()) {
                            destination = request.getGatewayDestination();
                        } else {
                            destination = request.getStages(nextSeq).getDestination();
                        }
                        servingServiceStub.withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, destination)
                                .serve(next, new EmptyStreamObserver() {
                                    @Override
                                    public void onError(Throwable t) {
                                        sendErrorMessage(next, t.getMessage());
                                    }
                                });
                    } else {
                        logger.error(ex.getMessage(), ex);
                        sendErrorMessage(request, ex.getMessage());
                    }
                });
    }

    private void sendErrorMessage(ServingPipeline request, String message) {
        logger.error("sendErrorMessage: {}, {}", request, message);

        ServingPipeline next = request.toBuilder()
                .setError(io.hydrosphere.serving.proto.Error.newBuilder()
                        .setErrorMessage(message)
                        .setSource(currentDestination)
                        .build()
                )
                .build();
        servingServiceStub.withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, request.getGatewayDestination())
                .serve(next, new EmptyStreamObserver());
    }
}
