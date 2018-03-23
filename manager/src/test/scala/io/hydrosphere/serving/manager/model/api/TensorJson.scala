package io.hydrosphere.serving.manager.model.api

import com.google.protobuf.ByteString
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec
import spray.json.{JsArray, JsNumber, JsObject, JsString}

class TensorJson extends WordSpec{
  "Tensors should convert to JSON" when {
    "classical TensorProto" in {
      val tensor = TensorProto(
        dtype = DataType.DT_STRING,
        tensorShape = TensorShape.fromSeq(Some(Seq(2,2))).toProto,
        stringVal = Seq("never", "gonna", "give", "you").map(ByteString.copyFromUtf8)
      )

      val expected = JsArray(
        JsArray(JsString("never"), JsString("gonna")),
        JsArray(JsString("give"), JsString("you"))
      )

      assert(tensor.jsonify === expected)
    }

    "TensorProto with [-1]" in {
      val tensor = TensorProto(
        dtype = DataType.DT_STRING,
        tensorShape = TensorShape.vector(-1).toProto,
        stringVal = Seq("never", "gonna", "give", "you").map(ByteString.copyFromUtf8)
      )

      val expected = JsArray(
        JsString("never"), JsString("gonna"),JsString("give"), JsString("you")
      )

      assert(tensor.jsonify === expected)
    }

    "TensorProto with maps" in {
      val tensor = TensorProto(
        dtype = DataType.DT_MAP,
        tensorShape = None,
        mapVal = Map(
          "name" -> TensorProto(
            dtype = DataType.DT_STRING,
            stringVal = Seq(ByteString.copyFromUtf8("Rick"))
          ),
          "email" -> TensorProto(
            dtype = DataType.DT_STRING,
            stringVal = Seq(ByteString.copyFromUtf8("rick@roll.com"))
          ),
          "age" -> TensorProto(
            dtype = DataType.DT_INT32,
            intVal = Seq(32)
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

      assert(tensor.jsonify === expected)
    }
  }
}
