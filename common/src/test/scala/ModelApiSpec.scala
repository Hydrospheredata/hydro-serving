import io.hydrosphere.serving.model_api._
import org.scalatest.WordSpec

class ModelApiSpec extends WordSpec {
  val dataFrame1 = DataFrame(
    List(
      ModelField(
        "features",
        FMatrix.vecVar(FDouble)
      ),
      ModelField(
        "indexedFeatures",
        FMatrix.vecVar(FInteger)
      )
    )
  )
  val dataFrame10 = DataFrame(
    List(
      ModelField(
        "features",
        FMatrix.vecVar(FDouble)
      )
    )
  )
  val dataFrame2 = DataFrame(
    List(
      ModelField(
        "text",
        FString
      ),
      ModelField(
        "indexedFeatures",
        FMatrix.vecVar(FInteger)
      )
    )
  )
  val dataFrame5 = DataFrame(
    List(
      ModelField(
        "tensor1",
        FMatrix(FDouble, List(-1, 3))
      ),
      ModelField(
        "indexedFeatures",
        FMatrix.vecVar(FInteger)
      )
    )
  )
  val dataFrame50 = DataFrame(
    List(
      ModelField(
        "tensor1",
        FMatrix(FDouble, List(-1, 3))
      )
    )
  )

  "ApiCompatibilityChecker" should {
    "allow" when {
      "UntypedApi -> UntypedAPI" in {
        assert(ApiCompatibilityChecker.check(UntypedAPI -> UntypedAPI) === true)
      }

      "DataFrame -> UntypedAPI" in {
        assert(ApiCompatibilityChecker.check(dataFrame1 -> UntypedAPI) === true)
      }

      "UntypedApi -> DataFrame" in {
        assert(ApiCompatibilityChecker.check(UntypedAPI -> dataFrame1) === true)
      }

      "DataFrame with itself" in {
        assert(ApiCompatibilityChecker.check(dataFrame1 -> dataFrame1) === true)
        assert(ApiCompatibilityChecker.check(dataFrame5 -> dataFrame5) === true)

      }

      "compatible DataFrames" in {
        assert(ApiCompatibilityChecker.check(dataFrame1 -> dataFrame10) === true)
        assert(ApiCompatibilityChecker.check(dataFrame5 -> dataFrame50) === true)
      }
    }

    "reject" when {
      "incompatible DataFrames" in {
        assert(ApiCompatibilityChecker.check(dataFrame10 -> dataFrame1) === false)
        assert(ApiCompatibilityChecker.check(dataFrame50 -> dataFrame5) === false)
      }
    }
  }

  "ModelApi" when {
    "UntypedAPI" should {
      "merge" when {
        "UntypedApi" in {
          assert(ModelApi.merge(UntypedAPI, UntypedAPI).isRight)
        }
        "DataFrame" in {
          assert(ModelApi.merge(dataFrame1, UntypedAPI).isRight)
          assert(ModelApi.merge(UntypedAPI, dataFrame10).isRight)
        }
      }
    }

    "DataFrame" should {
      "merge" when {
        "UntypedApi" in {
          assert(ModelApi.merge(dataFrame1, UntypedAPI).isRight)
          assert(ModelApi.merge(UntypedAPI, dataFrame10).isRight)
        }
        "DataFrame" in {
          assert(ModelApi.merge(dataFrame1, dataFrame10).isRight)
          assert(ModelApi.merge(dataFrame10, dataFrame1).isRight)
          assert(ModelApi.merge(dataFrame1, dataFrame10) === ModelApi.merge(dataFrame10, dataFrame1))
        }
      }
    }
  }
}
