package io.hydrosphere.serving.manager.controller

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import io.swagger.models.ExternalDocs

class SwaggerDocController(
  val apiClasses: Set[Class[_]]
)(
  implicit val actorSystem: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends SwaggerHttpService {

  override val info = Info(version = "1.0")
  override val externalDocs = Some(new ExternalDocs("ML Lambda", "https://github.com/Hydrospheredata/hydro-serving"))
}