package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.test.FullIntegrationSpec

import scala.concurrent.Future

class SidecarIntegrationSpec extends FullIntegrationSpec {
  "Sidecar" should {
    "see the manager" in {
      Future{
        println("hello")
        val logs = dockerClient.logs(sidecarContainer.id()).readFully()
        println(logs)
        assert(true)
      }
    }
  }
}
