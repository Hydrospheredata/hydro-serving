package io.hydrosphere.serving.manager.infrastructure.envoy.events

trait DiscoveryEventBus[F[_], T] {
  def detected(obj: T): F[Unit]

  def removed(obj: T): F[Unit]
}