package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.manager.GenericUnitTest

class ApplicationValidatorSpec extends GenericUnitTest {
  describe("ApplicationValidator") {
    it("should pass correct name") {
      val correctNames = Seq(
        "application",
        "123",
        "app_12",
        "application-test-version_12"
      )
      val results = correctNames.map(x => x -> ApplicationValidator.name(x).isDefined).toMap
      assert(results.forall(_._2 === true), results)
    }
    it("should reject incorrect name") {
      val incorrectNames = Seq(
        "_12$",
        "app!",
        "asad@",
        "azxc#",
        "$$$",
        "%",
        "",
        "^",
        "водка",
        ")(",
        "+*&"
      )
      val results = incorrectNames.map(x => x -> ApplicationValidator.name(x).isDefined).toMap
      assert(results.forall(_._2 === false), results)
    }
  }
}
