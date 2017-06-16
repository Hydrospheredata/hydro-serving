package io.hydrosphere.serving.service;

import io.grpc.*;

public class AuthorityReplacerInterceptor implements ClientInterceptor {

    public static final CallOptions.Key<String> DESTINATION_KEY = CallOptions.Key.of("destination", "localhost");

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {

        String destination = callOptions.getOption(DESTINATION_KEY);
        CallOptions newCallOptions = callOptions.withAuthority(destination);
        return next.newCall(method, newCallOptions);
    }
}
