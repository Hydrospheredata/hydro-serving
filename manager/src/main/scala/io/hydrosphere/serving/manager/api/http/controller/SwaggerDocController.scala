package io.hydrosphere.serving.manager.api.http.controller

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import io.swagger.models.ExternalDocs

class SwaggerDocController(
  val apiClasses: Set[Class[_]],
  val version: String
)(
  implicit val actorSystem: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends SwaggerHttpService  {

  override val apiDocsPath = "docs"
  override val info = Info(version = version)
  override val externalDocs = Some(new ExternalDocs("ML Lambda", "https://github.com/Hydrospheredata/hydro-serving"))
}