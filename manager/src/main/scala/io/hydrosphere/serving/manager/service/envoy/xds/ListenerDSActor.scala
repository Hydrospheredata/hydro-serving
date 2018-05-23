package io.hydrosphere.serving.manager.service.envoy.xds

import com.google.protobuf.struct.{Struct, Value}
import envoy.api.v2._
import envoy.api.v2.core.{Address, SocketAddress}
import envoy.api.v2.core.SocketAddress.PortSpecifier
import envoy.api.v2.listener.{Filter, FilterChain, FilterChainMatch}
import envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManager.Tracing.OperationName
import io.grpc.stub.StreamObserver


class ListenerDSActor extends AbstractDSActor[Listener](typeUrl = "type.googleapis.com/envoy.api.v2.Listener") {

  private val listeners =
    Seq(createListener(8080, OperationName.INGRESS), createListener(8081, OperationName.EGRESS))

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[Listener] =
    listeners

  private def createRDS(): Value =
    Value(Value.Kind.StructValue(Struct(Map(
      "route_config_name" -> Value(Value.Kind.StringValue(ROUTE_CONFIG_NAME)),
      "config_source" -> Value(Value.Kind.StructValue(Struct(Map(
        "ads" -> Value(Value.Kind.StructValue(Struct()))
      ))))
    ))))


  private def createTracing(operationName: OperationName): Value =
    Value(Value.Kind.StructValue(Struct(Map(
      "operation_name" -> Value(Value.Kind.StringValue(operationName.name))
    ))))

  private def createHttpFilter(name: String): Value =
    Value(Value.Kind.StructValue(Struct(Map(
      "name" -> Value(Value.Kind.StringValue(name))
    ))))

  private def createHttpConnectionManagerFilter(name: String, operationName: OperationName): Filter =
    Filter(
      name = "envoy.http_connection_manager",
      config = Some(Struct(
        Map(
          "codec_type" -> Value(Value.Kind.StringValue("AUTO")),
          "stat_prefix" -> Value(Value.Kind.StringValue(name)),
          "rds" -> createRDS(),
          "tracing" -> createTracing(operationName),
          "http_filters" -> Value(Value.Kind.ListValue(com.google.protobuf.struct.ListValue(Seq(
            createHttpFilter("envoy.router")
          ))))

        )
      ))
    )


  private def createListener(port: Int, operationName: OperationName): Listener = {
    Listener(
      name = operationName.name.toLowerCase,
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
          filters = Seq(createHttpConnectionManagerFilter(operationName.name.toLowerCase, operationName))
        )
      )
    )
  }
}
