package io.hydrosphere.serving.model_api

class ApiCompatibilityChecker(val modelApi: ModelApi) {
  def check(other: ModelApi): Boolean = {
    modelApi match {
      case DataFrame(apiDef) =>
        other match {
          case DataFrame(otherApiDef) =>
            val apiMap = apiDef.map(x => x.name -> x.fieldType).toMap
            otherApiDef.forall(field =>
              apiMap.get(field.name) match {
                case Some(x) => field.fieldType == x
                case None => false
              }
            )
          case UntypedAPI =>
            true
        }
      case UntypedAPI =>
        true
    }
  }
}

object ApiCompatibilityChecker {
  def apply(input: ModelApi): ApiCompatibilityChecker = new ApiCompatibilityChecker(input: ModelApi)
}
