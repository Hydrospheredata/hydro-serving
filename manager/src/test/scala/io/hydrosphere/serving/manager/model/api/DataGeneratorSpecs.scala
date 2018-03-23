package io.hydrosphere.serving.manager.model.api

import com.google.protobuf.ByteString
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.{ContractBuilders, DataGenerator}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor.{MapTensor, StringTensor, TensorProto}
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec

class DataGeneratorSpecs extends WordSpec {
  "DataGenerator" should {
    "generate correct example" when {
      "scalar flat signature" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        val expected = Map(
          "in1" -> TensorProto(dtype = DataType.DT_STRING, tensorShape = None, stringVal = List(ByteString.copyFromUtf8("foo")))
        )

        val generated = DataGenerator(sig1).generateInputs
        assert(generated === expected)
      }

      "vector flat signature" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, Some(List(-1))),
            ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, Some(List(3)))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        val expected = Map(
          "in1" -> TensorProto(dtype = DataType.DT_STRING, tensorShape = TensorShape.vector(-1).toProto, stringVal = List(ByteString.copyFromUtf8("foo"))),
          "in2" -> TensorProto(dtype = DataType.DT_INT32, tensorShape = TensorShape.vector(3).toProto, intVal = List(1, 1, 1))
        )

        val generated = DataGenerator(sig1).generateInputs
        assert(generated === expected)
      }

      "nested signature" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.complexField("in1",
              None,
              Seq(
                ContractBuilders.simpleTensorModelField("a", DataType.DT_STRING, None),
                ContractBuilders.simpleTensorModelField("b", DataType.DT_STRING, None)
              )
            )
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        val expected = Map(
          "in1" -> MapTensor(
            TensorShape.scalar,
            Seq(
              Map(
                "a" -> StringTensor(
                  TensorShape.scalar,
                  Seq("foo")
                ),
                "b" -> StringTensor(
                  TensorShape.scalar,
                  Seq("foo")
                )
              )
            )
          )
        )

        val generated = DataGenerator(sig1).generateInputs
        assert(generated === expected)
      }
    }
  }
}