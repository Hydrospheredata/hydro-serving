package io.hydrosphere.serving.model_api

object ApiCompatibilityChecker {
  def check(emitter: ModelApi, receiver: ModelApi): Boolean = {
    emitter match {
      case DataFrame(apiDef) =>
        receiver match {
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

  def check(apis: (ModelApi,  ModelApi)): Boolean = check(apis._1, apis._2)
}
