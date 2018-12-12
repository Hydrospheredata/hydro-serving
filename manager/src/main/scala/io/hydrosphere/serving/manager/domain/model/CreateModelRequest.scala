package io.hydrosphere.serving.manager.domain.model

case class CreateModelRequest(
  name: String,
)

case class UpdateModelRequest(
  id: Long,
  name: String,
) {
  def fillModel(model: Model) = {
    model.copy(
      id = id,
      name = name,
    )
  }
}