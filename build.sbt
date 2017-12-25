name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)

scalaVersion := Common.scalaCommonVersion

lazy val currentAppVersion = util.Properties.propOrElse("appVersion", "0.0.1")

lazy val currentSettings: Seq[Def.Setting[_]] = Seq(
  version := currentAppVersion,

  parallelExecution in Test := false,
  parallelExecution in IntegrationTest := false,

  fork in(Test, test) := true,
  fork in(IntegrationTest, test) := true,
  fork in(IntegrationTest, testOnly) := true
)

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(currentSettings)
  .settings(Common.settings)
  .aggregate(
    manager
  )

lazy val codegen = project.in(file("codegen"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.codegenDependencies)


lazy val manager = project.in(file("manager"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .dependsOn(codegen)
