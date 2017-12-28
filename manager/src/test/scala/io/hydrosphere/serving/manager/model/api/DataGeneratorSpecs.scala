package io.hydrosphere.serving.manager.model.api

import com.google.protobuf.ByteString
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec

class DataGeneratorSpecs extends WordSpec {
  "DataGenerator" should {
    "generate correct example" when {
      "scalar flat signature" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelContractBuilders.createTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
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
            ModelContractBuilders.createTensorModelField("in1", DataType.DT_STRING, Some(List(-1))),
            ModelContractBuilders.createTensorModelField("in2", DataType.DT_INT32, Some(List(3)))
          ),
          List(
            ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        val expected = Map(
          "in1" -> TensorProto(dtype = DataType.DT_STRING, tensorShape = Some(ModelContractBuilders.createTensorShape(List(-1))), stringVal = List(ByteString.copyFromUtf8("foo"))),
          "in2" -> TensorProto(dtype = DataType.DT_INT32, tensorShape = Some(ModelContractBuilders.createTensorShape(List(3))), intVal = List(1, 1, 1))
        )

        val generated = DataGenerator(sig1).generateInputs
        assert(generated === expected)
      }

      "nested signature" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelContractBuilders.createDictModelField("in1",
              Map(
                "a" -> ModelContractBuilders.createTensorInfo("a", DataType.DT_STRING, None),
                "b" -> ModelContractBuilders.createTensorInfo("b", DataType.DT_STRING, None)
              )
            )
          ),
          List(
            ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        val expected = Map(
          "in1" -> TensorProto(
            dtype = DataType.DT_MAP,
            tensorShape = None,
            mapVal = Map(
              "a" -> TensorProto(DataType.DT_STRING, None, stringVal = List(ByteString.copyFromUtf8("foo"))),
              "b" -> TensorProto(DataType.DT_STRING, None, stringVal = List(ByteString.copyFromUtf8("foo")))
            )
          )
        )

        val generated = DataGenerator(sig1).generateInputs
        assert(generated === expected)
      }
    }
  }
}