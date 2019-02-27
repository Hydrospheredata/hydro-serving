package io.hydrosphere.serving.manager.domain.model

object ModelValidator {
  def name(name: String) = {
    val validName = raw"^[a-zA-Z\-_\d]+$$".r
    if (validName.pattern.matcher(name).matches()) {
      Some(name)
    } else {
      None
    }
  }
}
