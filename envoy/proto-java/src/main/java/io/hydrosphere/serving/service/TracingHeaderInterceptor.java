package io.hydrosphere.serving.service;

import io.grpc.*;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 *
 */
public class TracingHeaderInterceptor implements ServerInterceptor, ClientInterceptor{
    public static final Context.Key<String> CTX_X_OT_SPAN_CONTEXT = Context.key("X-Ot-Span-Context");
    public static final Context.Key<String> CTX_X_REQUEST_ID = Context.key("X-Request-Id");
    public static final Context.Key<String> CTX_X_B3_TRACEID = Context.key("X-B3-TraceId");
    public static final Context.Key<String> CTX_X_B3_SPANID = Context.key("X-B3-SpanId");
    public static final Context.Key<String> CTX_X_B3_PARENTSPANID = Context.key("X-B3-ParentSpanId");
    public static final Context.Key<String> CTX_X_B3_SAMPLED = Context.key("X-B3-Sampled");
    public static final Context.Key<String> CTX_X_B3_FLAGS = Context.key("X-B3-Flags");

    private static final Metadata.Key<String> KEY_X_OT_SPAN_CONTEXT = Metadata.Key.of("X-Ot-Span-Context", ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_X_REQUEST_ID = Metadata.Key.of("X-Request-Id", ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_X_B3_TRACEID = Metadata.Key.of("X-B3-TraceId", ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_X_B3_SPANID = Metadata.Key.of("X-B3-SpanId", ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_X_B3_PARENTSPANID = Metadata.Key.of("X-B3-ParentSpanId", ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_X_B3_SAMPLED = Metadata.Key.of("X-B3-Sampled", ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_X_B3_FLAGS = Metadata.Key.of("X-B3-Flags", ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Context ctx = Context.current()
                .withValue(CTX_X_OT_SPAN_CONTEXT, metadata.get(KEY_X_OT_SPAN_CONTEXT))
                .withValue(CTX_X_REQUEST_ID, metadata.get(KEY_X_REQUEST_ID))
                .withValue(CTX_X_B3_TRACEID, metadata.get(KEY_X_B3_TRACEID))
                .withValue(CTX_X_B3_SPANID, metadata.get(KEY_X_B3_SPANID))
                .withValue(CTX_X_B3_PARENTSPANID, metadata.get(KEY_X_B3_PARENTSPANID))
                .withValue(CTX_X_B3_SAMPLED, metadata.get(KEY_X_B3_SAMPLED))
                .withValue(CTX_X_B3_FLAGS, metadata.get(KEY_X_B3_FLAGS));

        return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (CTX_X_OT_SPAN_CONTEXT.get() != null) {
                    headers.put(KEY_X_OT_SPAN_CONTEXT, CTX_X_OT_SPAN_CONTEXT.get());
                }
                if (CTX_X_REQUEST_ID.get() != null) {
                    headers.put(KEY_X_REQUEST_ID, CTX_X_REQUEST_ID.get());
                }
                if (CTX_X_B3_TRACEID.get() != null) {
                    headers.put(KEY_X_B3_TRACEID, CTX_X_B3_TRACEID.get());
                }
                if (CTX_X_B3_SPANID.get() != null) {
                    headers.put(KEY_X_B3_SPANID, CTX_X_B3_SPANID.get());
                }
                if (CTX_X_B3_PARENTSPANID.get() != null) {
                    headers.put(KEY_X_B3_PARENTSPANID, CTX_X_B3_PARENTSPANID.get());
                }
                if (CTX_X_B3_SAMPLED.get() != null) {
                    headers.put(KEY_X_B3_SAMPLED, CTX_X_B3_SAMPLED.get());
                }
                if (CTX_X_B3_FLAGS.get() != null) {
                    headers.put(KEY_X_B3_FLAGS, CTX_X_B3_FLAGS.get());
                }
                super.start(responseListener, headers);
            }
        };
    }
}