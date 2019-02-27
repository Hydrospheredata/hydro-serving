package io.hydrosphere.serving.manager.domain.image

import cats.syntax.either._

case class DockerImage(
  name: String,
  tag: String,
  sha256: Option[String] = None
) {
  
  def fullName: String = name + ":" + tag
  
  def replaceUser(user: String): Either[Throwable, DockerImage] = name match {
    case DockerImage.NamePattern(_, imageName) => DockerImage(s"$user/$imageName", tag, sha256).asRight
    case x => new Exception(s"Can't parse image name $x").asLeft
  }
}

object DockerImage {
  
  val NamePattern = "([a-zA-z0-9-_\\.:]+)/([a-zA-z0-9\\-_]+)".r
}