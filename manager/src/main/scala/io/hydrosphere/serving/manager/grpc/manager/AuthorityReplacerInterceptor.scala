package io.hydrosphere.serving.manager.grpc.manager

import io.grpc._

class AuthorityReplacerInterceptor extends ClientInterceptor {

  override def interceptCall[ReqT, RespT](
    method: MethodDescriptor[ReqT, RespT],
    callOptions: CallOptions,
    next: Channel
  ): ClientCall[ReqT, RespT] = {
    val destination = callOptions.getOption(AuthorityReplacerInterceptor.DESTINATION_KEY)
    val newCallOptions = callOptions.withAuthority(destination)
    next.newCall(method, newCallOptions)
  }

}

object AuthorityReplacerInterceptor {
  val DESTINATION_KEY: CallOptions.Key[String] = CallOptions.Key.of("destination", "localhost")
}