package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.model.api.json.TensorJsonLens
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor.{Int32Tensor, MapTensor, StringTensor}
import spray.json.{JsArray, JsNumber, JsObject, JsString}

class TensorJson extends GenericUnitTest{
  describe("TensorJsonLens") {
    it("should convert matrix[1, 2, 1, 2]") {
      val stensor = StringTensor(TensorShape.mat(1, 2, 1, 2), Seq("never", "gonna", "give", "you"))

      val expected = JsArray(
        JsArray(
          JsArray(
            JsArray(JsString("never"), JsString("gonna"))
          ),
          JsArray(
            JsArray(JsString("give"), JsString("you"))
          )
        )
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert matrix[2, 1, 1, 1, 1]") {
      val stensor = StringTensor(TensorShape.mat(2, 1, 1, 1, 1), Seq("never", "gonna"))

      val expected = JsArray(
        JsArray(JsArray(JsArray(JsArray(JsString("never"))))),
        JsArray(JsArray(JsArray(JsArray(JsString("gonna")))))
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert matrix[2,3]") {
      val stensor = StringTensor(TensorShape.mat(2, 3), Seq("never", "gonna", "give", "you", "up", "and"))

      val expected = JsArray(
        JsArray(JsString("never"), JsString("gonna"), JsString("give")),
        JsArray(JsString("you"), JsString("up"), JsString("and"))
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert matrix[1,4]") {
      val stensor = StringTensor(TensorShape.mat(1, 4), Seq("never", "gonna", "give", "you"))

      val expected = JsArray(
        JsArray(JsString("never"), JsString("gonna"), JsString("give"), JsString("you"))
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert matrix[-1,2]") {
      val stensor = StringTensor(TensorShape.mat(-1, 2), Seq("never", "gonna", "give", "you"))

      val expected = JsArray(
        JsArray(JsString("never"), JsString("gonna")),
        JsArray(JsString("give"), JsString("you"))
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert matrix[2,2]") {
      val stensor = StringTensor(TensorShape.mat(2, 2), Seq("never", "gonna", "give", "you"))

      val expected = JsArray(
        JsArray(JsString("never"), JsString("gonna")),
        JsArray(JsString("give"), JsString("you"))
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert vector[-1]") {
      val stensor = StringTensor(TensorShape.vector(-1) , Seq("never", "gonna", "give", "you"))

      val expected = JsArray(
        JsString("never"), JsString("gonna"),JsString("give"), JsString("you")
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert scalar") {
      val stensor = StringTensor(TensorShape.scalar , Seq("never"))
      val expected = JsString("never")
      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    it("should convert maps") {
      val stensor = MapTensor(
        shape = TensorShape.scalar,
        data = Seq(
          Map(
            "name" -> StringTensor(
              TensorShape.scalar,
              Seq("Rick")
            ),
            "email" -> StringTensor(
              TensorShape.scalar,
              Seq("rick@roll.com")
            ),
            "age" -> Int32Tensor(
              TensorShape.scalar,
              Seq(32)
            )
          )
        )
      )

      val expected = JsObject(
        Map(
          "name" -> JsString("Rick"),
          "email" -> JsString("rick@roll.com"),
          "age" -> JsNumber(32)
        )
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }
  }
}
