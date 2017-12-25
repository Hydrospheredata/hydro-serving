package io.hydrosphere.serving.model_api

sealed trait ModelType {
  def toTag: String
}

object ModelType {
  case class Tensorflow() extends ModelType {
    override def toTag: String = "tensorflow"
  }

  case class Spark private(version: String) extends ModelType {
    override def toTag: String = s"spark:$version"
  }

  case class Scikit() extends ModelType {
    override def toTag: String = "scikit"
  }

  case class PythonFunction(version: String) extends ModelType {
    override def toTag: String = s"python:$version"
  }

  case class Unknown() extends ModelType {
    override def toTag: String = "unknown"
  }

  def tryFromTag(tag: String): Option[ModelType] = {
    try {
      Some(fromTag(tag))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  def fromTag(tag: String): ModelType = {
    tag.split(':').toList match {
      case "tensorflow" :: Nil => Tensorflow()
      case "scikit" :: Nil => Scikit()
      case "unknown" :: Nil => Unknown()
      case "python" :: version :: Nil => PythonFunction(version)
      case "spark" :: modelVersion :: Nil =>
        val majors = modelVersion.split('.').take(2).mkString(".")
        Spark(majors)
      case _ => throw new IllegalArgumentException(s"Cant create a ModelTag from $tag")
    }
  }
}