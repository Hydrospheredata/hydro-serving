package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.WordSpec

class SignatureCheckerSpecs extends WordSpec {
  "SignatureChecker" should {
    "accept connection" when {

      "two identical signatures (String,String -> String,String)" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelField("in1", ModelField.InfoOrDict.Info(TensorInfo("in1", DataType.DT_STRING, None)))
          ),
          List(
            ModelField("out1", ModelField.InfoOrDict.Info(TensorInfo("out1", DataType.DT_STRING, None)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ModelField("out1", ModelField.InfoOrDict.Info(TensorInfo("out1", DataType.DT_STRING, None)))
          ),
          List(
            ModelField("out2", ModelField.InfoOrDict.Info(TensorInfo("out2", DataType.DT_STRING, None)))
          )
        )
        assert(SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two identical signatures (Double[5],Double[5] -> Double[5],Double[5])" in {
        val sig1 = ModelSignature(
          "sig1",
            ModelField("in1",
              ModelField.InfoOrDict.Info(
                TensorInfo(
                  "in1",
                  DataType.DT_DOUBLE,
                  Some(TensorShapeProto(TensorShapeProto.Dim(5) :: Nil))
                )
              )
            ) :: Nil,
          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_DOUBLE,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature(
          "sig2",
            ModelField("out1",
              ModelField.InfoOrDict.Info(
                TensorInfo(
                  "out1",
                  DataType.DT_DOUBLE,
                  Some(TensorShapeProto(TensorShapeProto.Dim(5) :: Nil))
                )
              )
            ) :: Nil,
          ModelField("out2",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out2",
                DataType.DT_DOUBLE,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: Nil))
              )
            )
          ) :: Nil
        )
        assert(SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two compatible signatures (Int32[3] -> Int32[-1])" in {
        val sig1 = ModelSignature(
          "sig1",

          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature(
          "sig2",

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(-1) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out2",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out2",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(-1) :: Nil))
              )
            )
          ) :: Nil
        )
        assert(SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two identical signatures (Double[5, 2] -> Double[5, 2])" in {
        val sig1 = ModelSignature(
          "sig1",

          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(2) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(2) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature(
          "sig2",

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(2) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out2",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out2",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(2) :: Nil))
              )
            )
          ) :: Nil
        )
        assert(SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two identical signatures (Double[5, 2] -> Double[5, -1])" in {
        val sig1 = ModelSignature(
          "sig1",

          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(2) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(2) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature(
          "sig2",

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(-1) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out2",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out2",
                DataType.DT_INT32,
                Some(TensorShapeProto(TensorShapeProto.Dim(5) :: TensorShapeProto.Dim(-1) :: Nil))
              )
            )
          ) :: Nil
        )
        assert(SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

    }

    "decline connection" when {
      "two completely different signatures (String -> Int32)" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ModelField("in1", ModelField.InfoOrDict.Info(TensorInfo("in1", DataType.DT_STRING, None)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ModelField("in2", ModelField.InfoOrDict.Info(TensorInfo("in2", DataType.DT_INT32, None)))
          )
        )
        assert(! SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two completely different signatures (String[3] -> String[4])" in {
        val sig1 = ModelSignature(
          "sig1",

          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature(
          "sig2",

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(4) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out2",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out2",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(4) :: Nil))
              )
            )
          ) :: Nil
        )
        assert(! SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two completely different signatures (Double[4] -> Double[3])" in {
        val sig1 = ModelSignature(
          "sig1",

          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(4) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(4) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature(
          "sig2",

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_DOUBLE,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out2",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out2",
                DataType.DT_DOUBLE,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil
        )
        assert(! SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two signatures when receiver has empty input signature" in {
        val sig1 = ModelSignature(
          "sig1",

          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil,

          ModelField("out1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "out1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature()
        assert(! SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }

      "two signatures when emitter has empty output signature" in {
        val sig1 = ModelSignature(
          "sig1",
          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil
        )
        val sig2 = ModelSignature(
          "sig2",
          ModelField("in1",
            ModelField.InfoOrDict.Info(
              TensorInfo(
                "in1",
                DataType.DT_STRING,
                Some(TensorShapeProto(TensorShapeProto.Dim(3) :: Nil))
              )
            )
          ) :: Nil
        )
        assert(! SignatureChecker.areSequentiallyCompatible(sig1, sig2))
      }
    }
  }
}
