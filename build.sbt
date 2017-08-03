name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)

val appVersion: SettingKey[String] = settingKey[String]("App version")
lazy val currentAppVersion = util.Properties.propOrElse("appVersion", "0.0.1")

lazy val currentSettings: Seq[Def.Setting[_]] = Seq(
  version := currentAppVersion
)

lazy val root = project.in(file("."))
  .settings(currentSettings)
  .settings(Common.settings)
  .aggregate(
    common,
    manager,
    gateway
  )

lazy val common = project.in(file("common"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)

lazy val codegen = project.in(file("codegen"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.codegenDependencies)

lazy val gateway = project.in(file("gateway"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)
  .dependsOn(common)

lazy val manager = project.in(file("manager"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .dependsOn(codegen, common)
