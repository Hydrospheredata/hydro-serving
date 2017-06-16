package io.hydrosphere.serving.service;

import io.grpc.stub.StreamObserver;
import io.hydrosphere.serving.proto.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class HealthServiceImpl extends HealthServiceGrpc.HealthServiceImplBase {

    private final Map<String, HealthChecker> checkers;

    public HealthServiceImpl(Map<String, HealthChecker> checkers) {
        if (checkers == null) {
            checkers = Collections.emptyMap();
        }
        this.checkers = checkers;
    }

    @Override
    public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
        Map<String, Health> statuses;
        HealthStatus healthStatus = HealthStatus.UP;
        if (checkers.isEmpty()) {
            statuses = Collections.emptyMap();
        } else {
            statuses = new HashMap<>();
            for (Map.Entry<String, HealthChecker> checker : checkers.entrySet()) {
                Health status = checker.getValue().health();
                if (status.getStatus() == HealthStatus.DOWN) {
                    healthStatus = HealthStatus.DOWN;
                }
                statuses.put(checker.getKey(), status);
            }
        }
        responseObserver.onNext(HealthResponse.newBuilder()
                .setStatus(healthStatus)
                .putAllHealth(statuses)
                .build());
        responseObserver.onCompleted();
    }
}
