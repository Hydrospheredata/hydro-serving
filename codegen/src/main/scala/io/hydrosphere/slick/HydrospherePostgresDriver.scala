package io.hydrosphere.slick

import com.github.tminglei.slickpg._

trait HydrospherePostgresDriver extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  {

  override val api = new MyAPI {}

  //////
  trait MyAPI extends API
    with ArrayImplicits
    with DateTimeImplicits
}

object HydrospherePostgresDriver extends HydrospherePostgresDriver
