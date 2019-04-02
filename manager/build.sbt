name := "serving-manager"
enablePlugins(BuildInfoPlugin, sbtdocker.DockerPlugin)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitCurrentBranch, git.gitCurrentTags, git.gitHeadCommit),
  buildInfoPackage := "io.hydrosphere.serving",
  buildInfoOptions += BuildInfoOption.ToJson
)

lazy val dockerSettings = Seq(
  imageNames in docker := Seq(ImageName(s"hydrosphere/serving-manager:${version.value}")),
  dockerfile in docker := {
    val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
    val classpath = (dependencyClasspath in Compile).value
    val dockerFilesLocation = baseDirectory.value / "src/main/docker/"
    val jarTarget = s"/hydro-serving/app/manager.jar"
    val osName = sys.props.get("os.name").getOrElse("unknown")

    new sbtdocker.Dockerfile {
      // Base image
      from("openjdk:8u151-jre-alpine")

      run("apk", "update")
      run("apk", "add", "jq")
      run("rm", "-rf", "/var/cache/apk/*")

      label("SERVICE_ID", "-20")
      label("HS_SERVICE_MARKER", "HS_SERVICE_MARKER")
      label("DEPLOYMENT_TYPE", "APP")
      label("RUNTIME_ID", "-20")
      label("SERVICE_NAME", "manager")

      add(dockerFilesLocation, "/hydro-serving/app/")
      // Add all files on the classpath
      add(classpath.files, "/hydro-serving/app/lib/")
      // Add the JAR file
      add(jarFile, jarTarget)

      if (osName.toLowerCase.startsWith("windows")) {
        run("dos2unix", "/hydro-serving/app/start.sh")
      }

      cmd("/hydro-serving/app/start.sh")
    }
  }
)

lazy val codegen = project.in(file("codegen"))

lazy val manager = project.in(file("."))
  .dependsOn(codegen)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(Common.settings)
  .settings(SlickGen.settings)
  .settings(ManagerDev.settings)
  .settings(buildInfoSettings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .settings(unmanagedSourceDirectories in Compile += sourceManaged.value)
  .settings(dockerSettings)
