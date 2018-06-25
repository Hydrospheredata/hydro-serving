package io.hydrosphere.serving.manager.model.api

import com.google.protobuf.ByteString
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor.{MapTensorData, TensorProto, TypedTensorFactory}
import io.hydrosphere.serving.tensorflow.types.DataType

class TensorExampleGeneratorSpec extends GenericUnitTest {
  val fooString = ByteString.copyFromUtf8("foo")

  describe("TensorExampleGenerator") {
    describe("should generate correct example") {
      it("when scalar flat signature") {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
          )
        )

        val expected = Map(
          "in1" -> TypedTensorFactory.create(
            TensorProto(
              dtype = DataType.DT_STRING,
              tensorShape = TensorShape.scalar.toProto,
              stringVal = List(fooString)
            )
          )
        )

        val generated = TensorExampleGenerator(sig1).inputs
        assert(generated === expected)
      }

      it("when vector flat signature") {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.vector(-1)),
            ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.vector(3))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
          )
        )

        val expected = Map(
          "in1" -> TypedTensorFactory.create(
            TensorProto(
              dtype = DataType.DT_STRING,
              tensorShape = TensorShape.vector(-1).toProto,
              stringVal = List(fooString)
            )
          ),
          "in2" -> TypedTensorFactory.create(
            TensorProto(
              dtype = DataType.DT_INT32,
              tensorShape = TensorShape.vector(3).toProto,
              intVal = List(1, 1, 1)
            )
          )
        )

        val generated = TensorExampleGenerator(sig1).inputs
        assert(generated === expected)
      }

      it("when nested singular signature") {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.complexField(
              "in1",
              TensorShape.scalar.toProto,
              Seq(
                ContractBuilders.simpleTensorModelField("a", DataType.DT_STRING, TensorShape.scalar),
                ContractBuilders.simpleTensorModelField("b", DataType.DT_STRING, TensorShape.scalar)
              )
            )
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
          )
        )

        val expected = Map(
          "in1" ->
            TypedTensorFactory.create(
              TensorProto(
                dtype = DataType.DT_MAP,
                tensorShape = TensorShape.scalar.toProto,
                mapVal = Seq(
                  MapTensorData(
                    Map(
                      "a" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString)),
                      "b" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString))
                    )
                  )
                )
              )
            )
        )

        val generated = TensorExampleGenerator(sig1).inputs
        assert(generated === expected)
      }

      it("when nested vector signature") {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.complexField(
              "in1",
              TensorShape.vector(3).toProto,
              Seq(
                ContractBuilders.simpleTensorModelField("a", DataType.DT_STRING, TensorShape.scalar),
                ContractBuilders.simpleTensorModelField("b", DataType.DT_STRING, TensorShape.scalar)
              )
            )
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
          )
        )

        val expected = Map(
          "in1" -> TypedTensorFactory.create(
            TensorProto(
              dtype = DataType.DT_MAP,
              tensorShape = TensorShape.vector(3).toProto,
              mapVal = Seq(
                MapTensorData(
                  Map(
                    "a" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString)),
                    "b" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString))
                  )
                ),
                MapTensorData(
                  Map(
                    "a" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString)),
                    "b" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString))
                  )
                ),
                MapTensorData(
                  Map(
                    "a" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString)),
                    "b" -> TensorProto(DataType.DT_STRING, TensorShape.scalar.toProto, stringVal = List(fooString))
                  )
                )
              )
            )
          )
        )

        val generated = TensorExampleGenerator(sig1).inputs
        assert(generated === expected)
      }
    }
  }
}