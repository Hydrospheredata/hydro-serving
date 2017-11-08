package io.hydrosphere.serving.streaming

import org.scalatest._

class KafkaDataFormatSpec extends FunSpec with Matchers {

  it("should convert input to request data") {
    val inputs = (1 to 10).map(i => s"""{"x": $i }""")
    val reqData = KafkaDataFormat.toRequestData(inputs)

    val expected = inputs.mkString("[", ",", "]").getBytes

    reqData shouldBe expected
  }

  it("should convert output to response messages") {
    val units = (1 to 3).map(i => s"""{"x":$i,"y":{"a":10,"b":"${i}dasdasd"}}""")
    val output = units.mkString("[", ",", "]").getBytes
    val messages = KafkaDataFormat.toResponseRecords(output)

    messages should contain theSameElementsAs(units)
  }
}
