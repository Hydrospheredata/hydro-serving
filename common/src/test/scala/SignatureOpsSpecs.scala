import hydroserving.contract.model_signature.ModelSignature
import hydroserving.tensorflow.types.DataType
import io.hydrosphere.serving.model_api.{ContractUtils, SignatureOps}
import org.scalatest.WordSpec

class SignatureOpsSpecs extends WordSpec {

  import SignatureOps.Implicits._

  "SignatureMerger" should {
    "merge" when {
      "signatures don't overlap" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ContractUtils.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractUtils.createTensorModelField("in2", DataType.DT_INT32, None)
          ),
          List(
            ContractUtils.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )

        val expectedSig = ModelSignature(
          "sig1&sig2",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_STRING, None),
            ContractUtils.createTensorModelField("in2", DataType.DT_INT32, None)
          ),
          List(
            ContractUtils.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
            ContractUtils.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )

        assert(sig1 +++ sig2 === expectedSig)
      }

      "inputs are overlapping and compatible" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_INT32, Some(List(-1)))
          ),
          List(
            ContractUtils.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_INT32, Some(List(3)))
          ),
          List(
            ContractUtils.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )

        val expectedSig = ModelSignature(
          "sig1&sig2",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_INT32, Some(List(3)))
          ),
          List(
            ContractUtils.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1))),
            ContractUtils.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )

        assert(sig1 +++ sig2 === expectedSig)
      }
    }

    "reject" when {
      "inputs overlap is conflicting" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ContractUtils.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_INT32, None)
          ),
          List(
            ContractUtils.createTensorModelField("out2", DataType.DT_INT32, Some(List(3)))
          )
        )

        assertThrows[IllegalArgumentException](sig1 +++ sig2)
      }

      "outputs overlap is conflicting" in {
        val sig1 = ModelSignature(
          "sig1",
          List(
            ContractUtils.createTensorModelField("in1", DataType.DT_STRING, None)
          ),
          List(
            ContractUtils.createTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
          )
        )
        val sig2 = ModelSignature(
          "sig2",
          List(
            ContractUtils.createTensorModelField("in2", DataType.DT_INT32, None)
          ),
          List(
            ContractUtils.createTensorModelField("out1", DataType.DT_INT32, Some(List(3)))
          )
        )

        assertThrows[IllegalArgumentException](sig1 +++ sig2)
      }
    }
  }
}
