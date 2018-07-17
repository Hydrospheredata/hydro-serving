package io.hydrosphere.serving.manager.model.db

case class ServiceKeyDescription(
  runtimeId: Long,
  modelVersionId: Option[Long],
  environmentId: Option[Long],
  modelName: Option[String] = None,
  runtimeName: Option[String] = None
) {
  def toServiceName(): String = s"r${runtimeId}m${modelVersionId.getOrElse(0)}e${environmentId.getOrElse(0)}"

  override def hashCode(): Int =
    scala.runtime.Statics.anyHash(ServiceKeyDescription.this.runtimeId)* 41 +
      scala.runtime.Statics.anyHash(ServiceKeyDescription.this.modelVersionId) * 41 +
      scala.runtime.Statics.anyHash(ServiceKeyDescription.this.environmentId)

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: ServiceKeyDescription =>
      this.environmentId == that.environmentId && this.runtimeId == that.runtimeId && this.modelVersionId == that.modelVersionId
    case _ => false
  }
}

object ServiceKeyDescription {
  val serviceKeyDescriptionPattern = """r(\d+)m(\d+)e(\d+)""".r

  def fromServiceName(name: String): Option[ServiceKeyDescription] = {
    name match {
      case serviceKeyDescriptionPattern(runtime, version, environment) =>
        val v = version.toLong
        val e = version.toLong

        Some(ServiceKeyDescription(
          runtimeId = runtime.toLong,
          modelVersionId = if (v <= 0) None else Some(v),
          environmentId = if (e <= 0) None else Some(e)
        ))
      case _ => None
    }
  }
}