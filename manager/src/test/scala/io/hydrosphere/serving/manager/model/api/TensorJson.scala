package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.manager.model.api.json.TensorJsonLens
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor.{Int32Tensor, MapTensor, StringTensor}
import org.scalatest.WordSpec
import spray.json.{JsArray, JsNumber, JsObject, JsString}

class TensorJson extends WordSpec{
  "Tensors should convert to JSON" when {
    "TensorProto matrix" in {
      val stensor = StringTensor(TensorShape.mat(2, 2), Seq("never", "gonna", "give", "you"))

      val expected = JsArray(
        JsArray(JsString("never"), JsString("gonna")),
        JsArray(JsString("give"), JsString("you"))
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    "TensorProto vector [-1]" in {
      val stensor = StringTensor(TensorShape.vector(-1) , Seq("never", "gonna", "give", "you"))

      val expected = JsArray(
        JsString("never"), JsString("gonna"),JsString("give"), JsString("you")
      )

      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    "TensorProto scalar" in {
      val stensor = StringTensor(TensorShape.scalar , Seq("never"))
      val expected = JsString("never")
      assert(TensorJsonLens.toJson(stensor) === expected)
    }

    "TensorProto with maps" in {
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
