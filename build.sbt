name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)

lazy val currentAppVersion = util.Properties.propOrElse("appVersion", "latest")

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(Common.settings)
  .aggregate(
    manager,
    dummyRuntime
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
