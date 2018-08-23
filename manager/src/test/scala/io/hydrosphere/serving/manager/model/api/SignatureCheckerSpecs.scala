package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.model.api.ops.ModelSignatureOps
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec

class SignatureCheckerSpecs extends WordSpec {
  "SignatureChecker" should {
    "accept connection" when {
      "two identical signatures (String,String -> String,String)" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_STRING, TensorShape.scalar)
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_STRING, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_STRING, TensorShape.scalar)
          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isDefined)
      }

      "two identical signatures (Double[5],Double[5] -> Double[5],Double[5])" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_DOUBLE, TensorShape.vector(5))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(5))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(5))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_DOUBLE, TensorShape.vector(5))

          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isDefined)
      }

      "two compatible signatures (Int32[3] -> Int32[-1])" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_INT32, TensorShape.vector(3))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_INT32, TensorShape.vector(3))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_INT32, TensorShape.vector(-1))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(-1))

          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isDefined)
      }

      "two identical signatures (Double[5, 2] -> Double[5, 2])" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_DOUBLE, TensorShape.mat(5, 2))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.mat(5, 2))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.mat(5, 2))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_DOUBLE, TensorShape.mat(5, 2))

          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isDefined)
      }

      "two compatible signatures (Double[5, 2] -> Double[5, -1])" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_DOUBLE, TensorShape.mat(5, 2))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.mat(5, 2))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.mat(5, -1))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_DOUBLE, TensorShape.mat(5, -1))

          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isDefined)
      }

    }

    "decline connection" when {
      "two completely different signatures (String -> Int32)" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_STRING, TensorShape.scalar)
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_INT32, TensorShape.scalar)
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.scalar)
          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isEmpty)
      }

      "two completely different signatures (String[3] -> String[4])" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, TensorShape.vector(3))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_STRING, TensorShape.vector(3))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_INT32, TensorShape.vector(4))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_INT32, TensorShape.vector(4))
          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isEmpty)
      }

      "two completely different signatures (Double[4] -> Double[3])" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_DOUBLE, TensorShape.vector(4))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(4))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(3))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_DOUBLE, TensorShape.vector(3))
          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isEmpty)
      }

      "two signatures when receiver has empty input signature" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_DOUBLE, TensorShape.vector(4))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, TensorShape.vector(4))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_DOUBLE, TensorShape.vector(4))
          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isEmpty)
      }

      "two signatures when emitter has empty output signature" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_DOUBLE, TensorShape.vector(4))
          ),
          List()
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractBuilders.simpleTensorModelField("in1", DataType.DT_DOUBLE, TensorShape.vector(4))
          ),
          List(
            ContractBuilders.simpleTensorModelField("out2", DataType.DT_DOUBLE, TensorShape.vector(4))
          )
        )
        assert(ModelSignatureOps.append(sig1, sig2).isEmpty)
      }
    }
  }
}