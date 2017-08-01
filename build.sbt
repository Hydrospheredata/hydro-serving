name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)
scalaVersion := "2.11.11"

lazy val root = project.in(file("."))
  .settings(Common.settings)
  .aggregate(
    common,
    manager,
    gateway
  )

lazy val common = project.in(file("common"))
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)

lazy val codegen = project.in(file("codegen"))
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.codegenDependencies)

lazy val gateway = project.in(file("gateway"))
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)
  .dependsOn(common)

lazy val manager = project.in(file("manager"))
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .dependsOn(codegen, common)
