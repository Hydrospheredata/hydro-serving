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
    sidecar,
    common,
    manager,
    gateway,
    streamingKafka
  )

lazy val sidecar = project.in(file("sidecar"))
  .settings(Common.settings)
  .settings(currentSettings)

lazy val hydroproto = project.in(file("hydro-serving-protos"))
  .settings(currentSettings)
  .settings(Common.settings)

lazy val common = project.in(file("common"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)
  .settings(
    libraryDependencies ++= Seq(
      "org.mockito" % "mockito-all" % "1.10.19" % "test",
      "org.scalactic" %% "scalactic" % Dependencies.scalaTestVersion % "test",
      "org.scalatest" %% "scalatest" % Dependencies.scalaTestVersion % "test"
    )
  )
  .dependsOn(hydroproto)


lazy val codegen = project.in(file("codegen"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.codegenDependencies)

lazy val gateway = project.in(file("gateway"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)
  .dependsOn(sidecar, common)

lazy val streamingKafka = project.in(file("streaming-kafka"))
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.streamingKafkaDependencies)
  .dependsOn(sidecar, common)

lazy val manager = project.in(file("manager"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(currentSettings)
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .dependsOn(sidecar, codegen, common)
