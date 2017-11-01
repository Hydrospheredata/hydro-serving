package io.hydrosphere.serving.model_api

trait ModelApi

case class DataFrame(definition: List[ModelField]) extends ModelApi

case object UntypedAPI extends ModelApi