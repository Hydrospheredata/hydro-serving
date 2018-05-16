package io.hydrosphere.serving.manager.model.api

sealed trait ModelType {
  def toTag: String
}

object ModelType {
  case class Tensorflow(version: String) extends ModelType {
    override def toTag: String = s"tensorflow:$version"
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

  case class Unknown(unknownType: String = "unknown", unknownTag: String = "unknown") extends ModelType {
    override def toTag: String = s"$unknownType:$unknownTag"
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
      case "tensorflow" :: version :: Nil => Tensorflow(version)
      case "scikit" :: Nil => Scikit()
      case "python" :: version :: Nil => PythonFunction(version)
      case "spark" :: modelVersion :: Nil =>
        val majors = modelVersion.split('.').take(2).mkString(".")
        Spark(majors)
      case unknownType :: unknownTag :: Nil => Unknown(unknownType, unknownTag)
      case x => Unknown("unknown", x.mkString)
    }
  }
}