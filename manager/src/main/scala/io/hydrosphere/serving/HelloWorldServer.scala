package io.hydrosphere.serving

import java.util.logging.Logger

import envoy.api.v2.{DiscoveryRequest, DiscoveryResponse, ListenerDiscoveryServiceGrpc}
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.{ExecutionContext, Future}

/**
  * [[https://github.com/grpc/grpc-java/blob/v0.15.0/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java]]
  */
object HelloWorldServer {
  private val logger = Logger.getLogger(classOf[HelloWorldServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new HelloWorldServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class HelloWorldServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(HelloWorldServer.port)
      .addService(ListenerDiscoveryServiceGrpc.bindService(new ListenerDiscoveryServiceGrpcImpl, executionContext)).build.start
    HelloWorldServer.logger.info("Server started, listening on " + HelloWorldServer.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class ListenerDiscoveryServiceGrpcImpl extends ListenerDiscoveryServiceGrpc.ListenerDiscoveryService {
    override def streamListeners(responseObserver: StreamObserver[DiscoveryResponse]): StreamObserver[DiscoveryRequest] =
      throw new RuntimeException

    override def fetchListeners(request: DiscoveryRequest): Future[DiscoveryResponse] = {
      Future.failed(new RuntimeException)
    }
  }

}
