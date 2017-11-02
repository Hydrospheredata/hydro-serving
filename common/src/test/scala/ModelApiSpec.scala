import io.hydrosphere.serving.model_api._
import org.scalatest.WordSpec

class ModelApiSpec extends WordSpec {
  val dataFrame1 = DataFrame.create(
    ModelField(
      "features",
      FMatrix.varvec(FDouble)
    ),
    ModelField(
      "indexedFeatures",
      FMatrix.varvec(FInteger)
    )
)

  val dataFrame10 = DataFrame.create(
    ModelField(
      "features",
      FMatrix.varvec(FDouble)
    )
  )

  val dataFrame2 = DataFrame.create(
    ModelField(
      "text",
      FString
    ),
    ModelField(
      "indexedFeatures",
      FMatrix.varvec(FInteger)
    )
  )


  val dataFrame5 = DataFrame.create(
    ModelField(
      "tensor1",
      FMatrix(FDouble, List(-1, 3))
    ),
    ModelField(
      "indexedFeatures",
      FMatrix.varvec(FInteger)
    )
  )

  val dataFrame50 = DataFrame.create(
    ModelField(
      "tensor1",
      FMatrix(FDouble, List(-1, 3))
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
}
