name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)

lazy val currentAppVersion = util.Properties.propOrElse("appVersion", "0.0.1")

lazy val currentSettings: Seq[Def.Setting[_]] = Seq(
  version := currentAppVersion
)

lazy val root = project.in(file("."))
  .settings(currentSettings)
  .settings(Common.settings)
  .aggregate(
    sidecar,
    common,
    manager,
    gateway
  )

lazy val sidecar = project.in(file("sidecar"))
  .settings(Common.settings)
  .settings(currentSettings)

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
  .dependsOn(sidecar, common)

lazy val manager = project.in(file("manager"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .dependsOn(sidecar, codegen, common)
