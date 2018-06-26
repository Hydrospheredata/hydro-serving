package io.hydrosphere.serving.manager.util.docker

case class ProgressDetail(
  current: Option[Long],
  start: Option[Long],
  total: Option[Long]
)
