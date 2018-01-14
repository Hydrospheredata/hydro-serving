package io.hydrosphere.serving.manager.service.envoy.xds

import com.google.protobuf.struct.{Struct, Value}
import envoy.api.v2.SocketAddress.PortSpecifier
import envoy.api.v2._
import io.grpc.stub.StreamObserver


class ListenerDSActor extends AbstractDSActor {

  private def createRDS(name: String): Value =
    Value(Value.Kind.StructValue(Struct(Map(
      "route_config_name" -> Value(Value.Kind.StringValue(name)),
      "config_source" -> Value(Value.Kind.StructValue(Struct(Map(
        "ads" -> Value(Value.Kind.StructValue(Struct()))
      ))))
    ))))

  private def createHttpFilter(name: String): Filter =
    Filter(
      name = "envoy.http_connection_manager",
      config = Some(Struct(
        Map(
          "codec_type" -> Value(Value.Kind.StringValue("AUTO")),
          "stat_prefix" -> Value(Value.Kind.StringValue(name)),
          "rds" -> createRDS(name)
        )
      ))
    )


  private def createListener(port: Int, name: String): Listener = {
    Listener(
      name = name,
      address = Some(Address(
        address = Address.Address.SocketAddress(
          SocketAddress(
            address = "0.0.0.0",
            portSpecifier = PortSpecifier.PortValue(port)
          )
        )
      )),
      filterChains = Seq(
        FilterChain(
          filterChainMatch = Some(FilterChainMatch(

          )),
          filters = Seq(createHttpFilter(name))
        )
      )
    )
  }

  private def sendListeners(stream: StreamObserver[DiscoveryResponse]): Unit = {
    val list = Seq(createListener(8080, "ingress"), createListener(8081, "egress"))

    send(DiscoveryResponse(
      typeUrl = "type.googleapis.com/envoy.api.v2.Listener",
      versionInfo = "0",
      resources = list.map(s => com.google.protobuf.any.Any.pack(s))
    ), stream)
  }

  override def receive: Receive = {
    case subscribe: SubscribeMsg =>
      observers += subscribe.responseObserver
      sendListeners(subscribe.responseObserver)

    case unsubcribe: UnsubscribeMsg =>
      observers -= unsubcribe.responseObserver

  }
}
