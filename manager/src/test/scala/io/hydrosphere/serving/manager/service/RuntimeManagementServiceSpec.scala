package io.hydrosphere.serving.manager.service

import org.scalatest.{FunSpec, Matchers}

class RuntimeManagementServiceSpec extends FunSpec with Matchers {

  it("should parse name and version") {

    import RuntimeManagementService._

    parseServiceName("binarizer_0-0-1") shouldBe Some(FullServiceName("binarizer", "0-0-1"))
    parseServiceName("binarizer_0.0.1") shouldBe Some(FullServiceName("binarizer", "0.0.1"))
    parseServiceName("binarizer_a") shouldBe None
  }
}
