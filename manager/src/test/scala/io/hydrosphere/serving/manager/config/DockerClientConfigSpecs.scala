package io.hydrosphere.serving.manager.config

import java.nio.file.Paths

import io.hydrosphere.serving.manager.GenericUnitTest

class DockerClientConfigSpecs extends GenericUnitTest {
  describe("DockerClient config parser") {
    it("should load config file") {
      val configRes = DockerClientConfig.load(getTestResourcePath("docker_configs/proxy_config.json"))
      assert(configRes.isSuccess, configRes)
      val config = configRes.get
      val default = config.proxies("default")
      assert(default.httpProxy.get === "http://localhost")
      assert(default.httpsProxy.get === "https://localhost")
      assert(default.noProxy.get === "noProxy")
      assert(default.ftpProxy.get === "ftpProxy")
    }

    it("should fall back to default if no file") {
      val configRes = DockerClientConfig.load(Paths.get("docker_configs/qweqwe.json"))
      assert(configRes.isFailure, configRes)
    }

    it("should fall back to default if incorrect file") {
      val configRes = DockerClientConfig.load(getTestResourcePath("docker_configs/no_proxy_config.json"))
      assert(configRes.isFailure, configRes)
    }
  }
}
