import sbt._
import sbt.Keys._

object ManagerDev {

  lazy val hostIp = taskKey[String]("Find IP")
  lazy val dockerEnv = taskKey[Unit]("Setup dev environment in docker")
  lazy val cleanDockerEnv = taskKey[Unit]("Stop images")
  lazy val devRun = taskKey[Unit]("Setup environment and ")

  lazy val pgImage = "postgres:9.6-alpine"
  lazy val pgName = "hs-dev-pg"

  lazy val uiImage = "hydrosphere/serving-manager-ui:api-v2"
  lazy val uiName = "hs-dev-ui"

  lazy val gatewayImage = "hydrosphere/serving-gateway:api-v2"
  lazy val gatewayName = "hs-dev-gateway"


  lazy val settings = Seq(
    hostIp := NetUtils.findLocalInetAddress().getHostAddress,
    dockerEnv := {
      val ip = hostIp.value
      streams.value.log.info(s"Host ip: $ip")

      val pg = DockerOpts(pgImage, pgName)
        .env("POSTGRES_DB", "docker")
        .env("POSTGRES_USER", "docker")
        .env("POSTGRES_PASSWORD", "docker")
        .exposePort(5432, 5432)

//      val gateway = DockerOpts(gatewayImage, gatewayName)
//        .env("SIDECAR_HOST", ip)
//        .exposePort(29091, 9091)
//        .exposePort(29090, 9090)

      val ui = DockerOpts(uiImage, uiName)
        .env("MANAGER_HOST", ip + ":9090")
        .exposePort(9098, 9091)
        .exposePort(8084, 80)

      def start(opts: DockerOpts): Unit = {
        containerId(opts.contrainerName) match {
          case Some(ContainerStatus(id, Stopped)) => startContainer(id)
          case None => runDocker(opts)
          case Some(ContainerStatus(_, Running)) =>
        }
      }

      start(pg)
      start(ui)
//      start(gateway)
    },
    cleanDockerEnv := {
      def stop(name: String): Unit = {
        containerId(name) match {
          case Some(ContainerStatus(id, Running)) =>
            stopContainer(id)
            removeContainer(id)
          case Some(ContainerStatus(id, Stopped)) =>
            removeContainer(id)
          case _ =>
        }
      }
      stop(pgName)
      stop(uiName)
      stop(gatewayName)
    },
    devRun := {
      val cp = (fullClasspath in Compile).value
      val s = streams.value
      val main = "io.hydrosphere.serving.manager.ManagerBoot"

      dockerEnv.value

      val modelsDir = target.value / "models"
      if (!modelsDir.exists()) IO.createDirectory(modelsDir)
      val runner = new ForkRun(ForkOptions().withRunJVMOptions(Vector(
        "-Dsidecar.host=127.0.0.1",
        s"-Dmanager.advertised-host=${hostIp.value}",
        "-Dmanager.advertised-port=9091",
        s"-Dlocal-storage.path=${modelsDir.getAbsolutePath}",
        s"-Dlocal-storage.name=localStorage",
        s"-Dcloud-driver.gateway.host=${hostIp.value}",
        s"-Dcloud-driver.gateway.port=29091",
        s"-Dcloud-driver.gateway.http-port=29090"
      )))
      runner.run(main, cp.files, Seq.empty, s.log)
    }
  )

  case class DockerOpts(
    imageName: String,
    envs: Map[String, String],
    portMappings: Map[Int, Int],
    contrainerName: String
  ) {

    def exposePort(from: Int, to: Int): DockerOpts = copy(portMappings = portMappings + (from -> to))
    def env(name: String, value: String): DockerOpts = copy(envs = envs + (name -> value))
    def name(name: String): DockerOpts = copy(contrainerName = name)
  }

  object DockerOpts {
    def apply(imageName: String, containerName: String): DockerOpts = DockerOpts(imageName, Map.empty, Map.empty, containerName)
  }

  private def runDocker(opts: DockerOpts): Unit = {
    def mkCommand(opts: DockerOpts): Seq[String] = {
      Seq("docker", "run") ++
        opts.envs.map({case (k,v) => s"$k=$v"}).flatMap(e => Seq("-e", e)) ++
        opts.portMappings.map({case (k, v) => s"$k:$v"}).flatMap(p => Seq("-p", p)) ++
        Seq("--name", opts.contrainerName) ++
        Seq("-d", opts.imageName)

    }
    import scala.sys.process._

    mkCommand(opts).!
  }

  private def startContainer(id: String): Unit = {
    import scala.sys.process._
    Seq("docker", "start", id).!
  }

  private def stopContainer(id: String): Unit = {
    import scala.sys.process._
    Seq("docker", "stop", id).!
  }

  private def removeContainer(id: String): Unit = {
    import scala.sys.process._
    Seq("docker", "rm", id).!
  }

  sealed trait Status
  case object Running extends Status
  case object Stopped extends Status

  case class ContainerStatus(id: String, status: Status)

  private def containerId(name: String): Option[ContainerStatus] = {
    import scala.sys.process._

    def psId(name: String, all: Boolean): Option[String] = {
      val base = Seq("docker", "ps")

      val dockerPs = if (all) base :+ "-a" else base
      val cmd = dockerPs #| Seq("grep", name) #| Seq("awk", "{print $1}")
      cmd.!!.split("\n").headOption
        .flatMap(s => {
          val cleanUp = s.trim
          if (cleanUp.isEmpty) None else Some(cleanUp)
        })
    }

    psId(name, false).map(id => ContainerStatus(id, Running))
      .orElse(psId(name, true).map(id => ContainerStatus(id, Stopped)))
  }

}

object NetUtils {

  import java.net.{Inet4Address, InetAddress, NetworkInterface}
  import scala.collection.JavaConverters._

  /**
    * Copied from spark - see org.apache.spark.utils.Utils.findLocalInetAddress
    * windows related thing removed
    */
  def findLocalInetAddress(): InetAddress = {
    val address = InetAddress.getLocalHost
    if (address.isLoopbackAddress) {
      val activeNetworkIFs = NetworkInterface.getNetworkInterfaces.asScala.toList.reverse
      for (ni <- activeNetworkIFs) {
        val addresses = ni.getInetAddresses.asScala.toList
          .filterNot(addr => addr.isLinkLocalAddress || addr.isLoopbackAddress)
        if (addresses.nonEmpty) {
          val addr = addresses.find(_.isInstanceOf[Inet4Address]).getOrElse(addresses.head)
          val strippedAddress = InetAddress.getByAddress(addr.getAddress)
          return strippedAddress
        }
      }
    }
    address
  }
}
