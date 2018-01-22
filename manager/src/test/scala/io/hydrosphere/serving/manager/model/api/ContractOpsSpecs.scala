package io.hydrosphere.serving.manager.model.api

import com.google.protobuf.ByteString
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.ContractOps.{ContractDescription, FieldDescription, SignatureDescription}
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec
import spray.json.{JsArray, JsNumber, JsObject, JsString}


class ContractOpsSpecs extends WordSpec {

  import io.hydrosphere.serving.manager.model.api.ContractOps.Implicits._

  "ContractOps" can {
    "merge" should {
      "success" when {
        "signatures don't overlap" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          val expectedSig = ModelSignature(
            "sig1&sig2",
            List(
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None),
              ModelContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
              ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          assert(sig1 +++ sig2 === expectedSig)
        }

        "inputs are overlapping and compatible" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, Some(List(-1)))
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, Some(List(3)))
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          val expectedSig = ModelSignature(
            "sig1&sig2",
            List(
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, Some(List(3)))
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
              ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
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
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
            )
          )

          assertThrows[IllegalArgumentException](sig1 +++ sig2)
        }

        "outputs overlap is conflicting" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ModelContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, None)
            ),
            List(
              ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_INT32, Some(List(3)))
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
            ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ModelContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, None)
          ),
          List(
            ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )
        val contract = ModelContract("test", List(sig1, sig2))

        val expected = ContractDescription(
          List(
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
        )

        assert(contract.flatten === expected)
      }

      "contract is nested" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelContractBuilders.complexField(
              "in",
              Seq(
                ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None),
                ModelContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, None)
              )
            )
          ),
          List(
            ModelContractBuilders.complexField(
              "out",
              Seq(
                ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
                ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, None)
              )
            )
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ModelContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, None)
          ),
          List(
            ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )
        val contract = ModelContract("test", List(sig1, sig2))

        val expected = ContractDescription(
          List(
            SignatureDescription(
              "sig1",
              inputs = List(
                FieldDescription("/in/in1", DataType.DT_STRING, None),
                FieldDescription("/in/in2", DataType.DT_INT32, None)
              ),
              outputs = List(
                FieldDescription("/out/out1", DataType.DT_DOUBLE, Some(List(-1))),
                FieldDescription("/out/out2", DataType.DT_INT32, None)
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
        )

        assert(contract.flatten === expected)
      }
    }

    "unflatten" when {
      "non-nested contract" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        val flatSignature = SignatureDescription(
          "sig1",
          inputs = List(
            FieldDescription("/in1", DataType.DT_STRING, None)
          ),
          outputs = List(
            FieldDescription("/out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        assert(ContractOps.SignatureDescription.toSignature(flatSignature) === sig1)
      }
      "contract is nested" in {
        val signature = ModelSignature(
          "sig1",
          List(
            ModelContractBuilders.complexField(
              "in",
              Seq(
                ModelContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None),
                ModelContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, None)
              )
            )
          ),
          List(
            ModelContractBuilders.complexField(
              "out",
              Seq(
                ModelContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
                ModelContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, None)

              )
            )
          )
        )

        val signatureDescription = SignatureDescription(
            "sig1",
            inputs = List(
              FieldDescription("/in/in1", DataType.DT_STRING, None),
              FieldDescription("/in/in2", DataType.DT_INT32, None)
            ),
            outputs = List(
              FieldDescription("/out/out1", DataType.DT_DOUBLE, Some(List(-1))),
              FieldDescription("/out/out2", DataType.DT_INT32, None)
            )
          )

        val restored = ContractOps.SignatureDescription.toSignature(signatureDescription)

        assert(restored === signature)
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
