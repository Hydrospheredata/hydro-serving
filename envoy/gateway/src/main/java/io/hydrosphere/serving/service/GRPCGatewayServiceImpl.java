package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import io.hydrosphere.serving.proto.Empty;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.proto.ServingServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class GRPCGatewayServiceImpl extends ServingServiceGrpc.ServingServiceImplBase {
    private final static Logger logger = LoggerFactory.getLogger(GRPCGatewayServiceImpl.class);

    private final ConcurrentHashMap<String, DeferredResult<JsonNode>> deferredResults = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ServingServiceGrpc.ServingServiceStub servingService;

    public GRPCGatewayServiceImpl(ServingServiceGrpc.ServingServiceStub servingService) {
        this.servingService = servingService;
    }

    public DeferredResult<JsonNode> sendToMesh(ServingPipeline pipeline, Map<String, String> headers) {
        logger.info("io.hydrosphere.serving.service.GRPCGatewayServiceImpl.sendToMesh: {}", pipeline);

        DeferredResult<JsonNode> result = new DeferredResult<>();
        deferredResults.put(pipeline.getRequestId(), result);
        result.onTimeout(() -> deferredResults.remove(pipeline.getRequestId()));

        servingService.withOption(AuthorityReplacerInterceptor.DESTINATION_KEY,
                pipeline.getStagesList().get(0).getDestination())
                .serve(pipeline, new EmptyStreamObserver(){
            @Override
            public void onNext(Empty value) {
                logger.info("io.hydrosphere.serving.service.GRPCGatewayServiceImpl.sendToMesh#observer/onNext: {}", value);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("io.hydrosphere.serving.service.GRPCGatewayServiceImpl.sendToMesh#observer/onError",t);
            }

            @Override
            public void onCompleted() {
                logger.info("io.hydrosphere.serving.service.GRPCGatewayServiceImpl.sendToMesh#observer/onCompleted");
            }
        });
        return result;
    }

    @Override
    public void serve(ServingPipeline request, StreamObserver<Empty> responseObserver) {
        logger.info("io.hydrosphere.serving.service.GRPCGatewayServiceImpl.serve: {}", request);
        responseObserver.onCompleted();

        DeferredResult<JsonNode> result = deferredResults.get(request.getRequestId());
        if (result != null) {
            if (request.hasError()) {
                logger.error("Error message {}", request);
                result.setErrorResult(request.getError().getErrorMessage());
            } else {
                try {
                    JsonNode node = objectMapper.readTree(request.getData().toByteArray());
                    result.setResult(node);
                } catch (IOException e) {
                    result.setErrorResult(e);
                }
            }
            deferredResults.remove(request.getRequestId());
        } else {
            logger.warn("Can't find DeferredResult result for id {}", request.getRequestId());
        }
    }
}
