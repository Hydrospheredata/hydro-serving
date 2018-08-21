import microsites.{ConfigYml, MicrositeFavicon}

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

lazy val codegen = project.in(file("codegen"))
  .settings(exportJars := true)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.codegenDependencies)

lazy val manager = project.in(file("manager"))
  .dependsOn(codegen)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .settings(SlickGen.settings: _*)
  .settings(ManagerDev.settings: _*)

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
