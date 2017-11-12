package io.hydrosphere.serving.streaming

import akka.http.scaladsl.model.StatusCodes
import io.hydrosphere.serving.connector._
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class SidecarServingProcessorSpec extends FunSpec with Matchers {

  it("should process success") {
    val connector = constConnector(ExecutionSuccess("[{\"y\":1}]".getBytes))
    val processor = SidecarServingProcessor(connector, "test")

    val input = Seq("{\"x\":1}","{\"x\":2}")
    val result = Await.result(processor.serve(input), Duration.Inf)
    result shouldBe Seq("{\"y\":1}")
  }

  it("should process failure") {
    import spray.json._
    import ErrorJsonEncoding._

    val connector = constConnector(ExecutionFailure("test error", StatusCodes.InternalServerError))
    val processor = SidecarServingProcessor(connector, "test")

    val input = Seq("{\"x\":1}","{\"x\":2}")
    val result = Await.result(processor.serve(input), Duration.Inf)

    val expected = Seq(
      ErrorConsumerRecord("{\"x\":1}", "test error"),
      ErrorConsumerRecord("{\"x\":2}", "test error")
    ).map(e => e.toJson.compactPrint)

    result shouldBe expected
  }

  def constConnector(f: => ExecutionResult): RuntimeMeshConnector = {
    new RuntimeMeshConnector {
      override def execute(command: ExecutionCommand): Future[ExecutionResult] =
        Future.successful(f)
    }
  }
}
