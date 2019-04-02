enablePlugins(ParadoxPlugin)
enablePlugins(ParadoxMaterialThemePlugin)

lazy val docs = project.in(file("."))
  .settings(Common.settings)
  .settings(
    name := "Hydrosphere Serving",
    paradoxProperties in Compile ++= Map(
      "github.base_url" -> s"https://github.com/Hydrospheredata/hydro-serving/tree/${version.value}"
    ),
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    git.remoteRepo := "git@github.com:Hydrospheredata/hydro-serving.git",
  )