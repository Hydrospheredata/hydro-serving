package io.hydrosphere.serving.service;

import io.grpc.stub.StreamObserver;
import io.hydrosphere.serving.proto.Empty;

/**
 *
 */
public class EmptyStreamObserver implements StreamObserver<Empty> {
    @Override
    public void onNext(Empty value) {

    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onCompleted() {

    }
}
