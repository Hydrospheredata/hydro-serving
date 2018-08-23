package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.model.api.description.{ContractDescription, FieldDescription, SignatureDescription}
import io.hydrosphere.serving.model.api.ops.ModelSignatureOps
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec


class ContractOpsSpecs extends WordSpec {

  import io.hydrosphere.serving.model.api.ops.Implicits._

  "ContractOps" can {
    "merge" should {
      "success" when {
        "empty and non-empty" in {
          val sig1 = ModelSignature(
            "sig1",
            List.empty,
            List.empty
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          val expectedSig = ModelSignature(
            "sig1&sig2",
            List(
              ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          assert(ModelSignatureOps.merge(sig1, sig2) === expectedSig)
        }

        "signatures don't overlap" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          val expectedSig = ModelSignature(
            "sig1&sig2",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar),
              ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1)),
              ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          assert(ModelSignatureOps.merge(sig1, sig2) === expectedSig)
        }

        "inputs are overlapping and compatible" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, TensorShape.vector(-1))
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, TensorShape.vector(3))
            ),
            List(
              ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          val expectedSig = ModelSignature(
            "sig1&sig2",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, TensorShape.vector(3))
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1)),
              ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          assert(ModelSignatureOps.merge(sig1, sig2) === expectedSig)
        }
      }

      "fail" when {
        "inputs overlap is conflicting" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          assertThrows[IllegalArgumentException](ModelSignatureOps.merge(sig1, sig2))
        }

        "outputs overlap is conflicting" in {
          val sig1 = ModelSignature(
            "sig1",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
            )
          )
          val sig2 = ModelSignature(
            "sig2",
            List(
              ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_INT32, TensorShape.vector(3))
            )
          )

          assertThrows[IllegalArgumentException](ModelSignatureOps.merge(sig1, sig2))
        }
      }
    }

    "flatten" when {
      "contract is flat" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
          )
        )
        val contract = ModelContract("test", Seq(sig1, sig2))

        val expected = ContractDescription(
          List(
            SignatureDescription(
              "sig1",
              inputs = List(
                FieldDescription("/in1", DataType.DT_STRING, Some(Seq.empty))
              ),
              outputs = List(
                FieldDescription("/out1", DataType.DT_DOUBLE, Some(List(-1)))
              )
            ),
            SignatureDescription(
              "sig2",
              inputs = List(
                FieldDescription("/in2", DataType.DT_INT32, Some(Seq.empty))
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
            ContractBuilders.complexField(
              "in",
              None,
              Seq(
                ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar),
                ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
              )
            )
          ),
          List(
            ContractBuilders.complexField(
              "out",
              None,
              Seq(
                ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1)),
                ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.scalar)
              )
            )
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(3))
          )
        )
        val contract = ModelContract("test", Seq(sig1, sig2))

        val expected = ContractDescription(
          List(
            SignatureDescription(
              "sig1",
              inputs = List(
                FieldDescription("/in/in1", DataType.DT_STRING, Some(Seq.empty)),
                FieldDescription("/in/in2", DataType.DT_INT32, Some(Seq.empty))
              ),
              outputs = List(
                FieldDescription("/out/out1", DataType.DT_DOUBLE, Some(List(-1))),
                FieldDescription("/out/out2", DataType.DT_INT32, Some(Seq.empty))
              )
            ),
            SignatureDescription(
              "sig2",
              inputs = List(
                FieldDescription("/in2", DataType.DT_INT32, Some(Seq.empty))
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
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1))
          )
        )

        val flatSignature = SignatureDescription(
          "sig1",
          inputs = List(
            FieldDescription("/in1", DataType.DT_STRING, Some(Seq.empty))
          ),
          outputs = List(
            FieldDescription("/out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )

        assert(SignatureDescription.toSignature(flatSignature) === sig1)
      }
      "contract is nested" in {
        val signature = ModelSignature(
          "sig1",
          List(
            ContractBuilders.complexField(
              "in",
              None,
              Seq(
                ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar),
                ContractBuilders.simpleTensorModelField("in2", DataType.DT_INT32, TensorShape.scalar)
              )
            )
          ),
          List(
            ContractBuilders.complexField(
              "out",
              None,
              Seq(
                ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(-1)),
                ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.scalar)
              )
            )
          )
        )

        val signatureDescription = SignatureDescription(
          "sig1",
          inputs = List(
            FieldDescription("/in/in1", DataType.DT_STRING, Some(Seq.empty)),
            FieldDescription("/in/in2", DataType.DT_INT32, Some(Seq.empty))
          ),
          outputs = List(
            FieldDescription("/out/out1", DataType.DT_DOUBLE, Some(List(-1))),
            FieldDescription("/out/out2", DataType.DT_INT32, Some(Seq.empty))
          )
        )

        val restored = SignatureDescription.toSignature(signatureDescription)

        assert(restored === signature)
      }
    }

  }
}