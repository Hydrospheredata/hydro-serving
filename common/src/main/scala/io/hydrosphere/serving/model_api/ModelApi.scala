package io.hydrosphere.serving.model_api

trait ModelApi

case class DataFrame(definition: List[ModelField]) extends ModelApi { }
object DataFrame {
  def create(modelFields: ModelField*) = DataFrame(modelFields.toList)
}

case object UntypedAPI extends ModelApi