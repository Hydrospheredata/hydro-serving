import microsites.{ConfigYml, MicrositeFavicon}

import DevTasks._

name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(Common.settings)
  .aggregate(
    manager,
    docs,
    dummyRuntime
  )
  .settings(DevTasks.settings: _*)
  .settings(
    devRun := {
      val cp = fullClasspath.in(manager, Compile).value
      val s = streams.value
      val main = "io.hydrosphere.serving.manager.ManagerBoot"

      DevTasks.dockerEnv.value

      val modelsDir = target.value / "models"
      if (!modelsDir.exists()) IO.createDirectory(modelsDir)
      val runner = new ForkRun(ForkOptions().withRunJVMOptions(Vector(
        "-Dsidecar.host=127.0.0.1",
        s"-Dmanager.advertisedHost=${DevTasks.hostIp.value}",
        "-Dmanager.advertisedPort=9091",
        s"-DlocalStorage.path=${modelsDir.getAbsolutePath}"
      )))
      runner.run(main, cp.files, Seq.empty, s.log)
    }
  )

lazy val codegen = project.in(file("codegen"))
  .settings(exportJars := true)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.codegenDependencies)

lazy val manager = project.in(file("manager"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .dependsOn(codegen)


lazy val dummyRuntime = project.in(file("dummy-runtime"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingDummyRuntimeDependencies)

lazy val docs = project.in(file("docs"))
  .enablePlugins(MicrositesPlugin)
  .dependsOn(manager)
  .settings(Common.settings)
  .settings(
    micrositeName := "ML Lambda",
    micrositeDescription := "Machine Learning Serving Сluster",
    micrositeAuthor := "hydrosphere.io",
    micrositeHighlightTheme := "atom-one-light",
    micrositeDocumentationUrl := "index.html",
    micrositeGithubOwner := "Hydrospheredata",
    micrositeGithubRepo := "hydro-serving",
    micrositeBaseUrl := "/serving-docs",
    micrositeTwitter := "@hydrospheredata",
    micrositeTwitterCreator := "@hydrospheredata",
      micrositePalette := Map(
      "brand-primary" -> "#052150",
      "brand-secondary" -> "#081440",
      "brand-tertiary" -> "#052150",
      "gray-dark" -> "#48494B",
      "gray" -> "#7D7E7D",
      "gray-light" -> "#E5E6E5",
      "gray-lighter" -> "#F4F3F4",
      "white-color" -> "#FFFFFF"),
    ghpagesNoJekyll := false,
    git.remoteRepo := "git@github.com:Hydrospheredata/hydro-serving.git",
    micrositeConfigYaml := ConfigYml(
      yamlCustomProperties = Map("version" -> version.value)
    ),
    micrositeFavicons := {
      Seq("16", "32", "48", "72", "96", "192", "194").map(s => {
        val size = s + "x" + s
        MicrositeFavicon(s"favicon-$size.png", size)
      })
    }
  )
