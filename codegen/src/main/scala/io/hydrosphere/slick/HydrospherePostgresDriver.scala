package io.hydrosphere.slick

import com.github.tminglei.slickpg._

trait HydrospherePostgresDriver extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgSprayJsonSupport {

  def pgjson = "jsonb"

  override val api = new MyAPI {}

  //////
  trait MyAPI extends API
    with ArrayImplicits
    with DateTimeImplicits
    with JsonImplicits

}

object HydrospherePostgresDriver extends HydrospherePostgresDriver
