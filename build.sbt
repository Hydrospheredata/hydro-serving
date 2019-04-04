name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(Common.settings)
  .aggregate(
    manager,
    docs
  )

lazy val manager = project.in(file("manager"))

lazy val docs = project.in(file("docs"))
  .dependsOn(manager)