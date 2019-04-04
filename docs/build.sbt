enablePlugins(ParadoxPlugin, ParadoxMaterialThemePlugin)
lazy val paradoxSettings = Seq(
    paradoxProperties in Compile ++= Map(
      "github.base_url" -> s"https://github.com/Hydrospheredata/hydro-serving/tree/${version.value}"
    ),
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    git.remoteRepo := "git@github.com:Hydrospheredata/hydro-serving.git",
)

lazy val docs = project.in(file("."))
  .settings(Settings.all)
	.settings(paradoxSettings)
