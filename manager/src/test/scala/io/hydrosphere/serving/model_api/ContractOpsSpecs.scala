package io.hydrosphere.serving.model_api

import com.google.protobuf.ByteString
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.model_api.ContractOps.{FieldDescription, SignatureDescription}
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec
import spray.json.{JsArray, JsNumber, JsObject, JsString}


class ContractOpsSpecs extends WordSpec {

  import ContractOps.Implicits._

  "ContractOps" can {
    "merge" should {
      "success" when {
        "signatures don't overlap" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.createTensorModelField("in2", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          val expectedSig = ModelSignature(
            "sig1&sig2",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_STRING, None),
              ModelContractBuilders.createTensorModelField("in2", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
              ModelContractBuilders.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          assert(sig1 +++ sig2 === expectedSig)
        }

        "inputs are overlapping and compatible" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_INT32, Some(List(-1)))
            ),
            List(
              ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_INT32, Some(List(3)))
            ),
            List(
              ModelContractBuilders.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          val expectedSig = ModelSignature(
            "sig1&sig2",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_INT32, Some(List(3)))
            ),
            List(
              ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
              ModelContractBuilders.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          assert(sig1 +++ sig2 === expectedSig)
        }
      }

      "fail" when {
        "inputs overlap is conflicting" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          assertThrows[IllegalArgumentException](sig1 +++ sig2)
        }

        "outputs overlap is conflicting" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ModelContractBuilders.createTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.createTensorModelField("in2", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.createTensorModelField("out1", DataType.DT_INT32, Some(List(3)))
            )
          )

          assertThrows[IllegalArgumentException](sig1 +++ sig2)
        }
      }
    }

    "flatten" when {
      "contract is flat" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelContractBuilders.createTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ModelContractBuilders.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ModelContractBuilders.createTensorModelField("in2", DataType.DT_INT32, None)
          ),
          List(
            ModelContractBuilders.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )
        val contract = ModelContract("test",List(sig1, sig2))

        val expected = List(
          SignatureDescription(
            "sig1",
            inputs = List(
              FieldDescription("/in1", DataType.DT_STRING, None)
            ),
            outputs = List(
              FieldDescription("/out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          ),
          SignatureDescription(
            "sig2",
            inputs = List(
              FieldDescription("/in2", DataType.DT_INT32, None)
            ),
            outputs = List(
              FieldDescription("/out2", DataType.DT_INT32, Some(List(3)))
            )
          )
        )

        assert(contract.flatten === expected)
      }

      "contract is nested" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelContractBuilders.createDictModelField(
              "in",
              Map(
                "a" -> ModelContractBuilders.createTensorInfo("in1", DataType.DT_STRING, None),
                "b" -> ModelContractBuilders.createTensorInfo("in2", DataType.DT_INT32, None)
              )
            )
          ),
          List(
            ModelContractBuilders.createDictModelField(
              "out",
              Map(
                "x" -> ModelContractBuilders.createTensorInfo("out1", DataType.DT_DOUBLE, Some(List(-1))),
                "y" -> ModelContractBuilders.createTensorInfo("out2", DataType.DT_INT32, None)
              )
            )
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ModelContractBuilders.createTensorModelField("in2", DataType.DT_INT32, None)
          ),
          List(
            ModelContractBuilders.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )
        val contract = ModelContract("test",List(sig1, sig2))

        val expected = List(
          SignatureDescription(
            "sig1",
            inputs = List(
              FieldDescription("/in/a/in1", DataType.DT_STRING, None),
              FieldDescription("/in/b/in2", DataType.DT_INT32, None)
            ),
            outputs = List(
              FieldDescription("/out/x/out1", DataType.DT_DOUBLE, Some(List(-1))),
              FieldDescription("/out/y/out2", DataType.DT_INT32, None)
            )
          ),
          SignatureDescription(
            "sig2",
            inputs = List(
              FieldDescription("/in2", DataType.DT_INT32, None)
            ),
            outputs = List(
              FieldDescription("/out2", DataType.DT_INT32, Some(List(3)))
            )
          )
        )

        assert(contract.flatten === expected)
      }
    }

    "jsonify" when {
      "classical TensorProto" in {
        val tensor = TensorProto(
          dtype = DataType.DT_STRING,
          tensorShape = Some(ModelContractBuilders.createTensorShape(Seq(2,2))),
          stringVal = Seq("never", "gonna", "give", "you").map(ByteString.copyFromUtf8)
        )

        val expected = JsArray(
          JsArray(JsString("never"), JsString("gonna")),
          JsArray(JsString("give"), JsString("you"))
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
}
