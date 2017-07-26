name := "hydro-serving"

updateOptions := updateOptions.value.withCachedResolution(true)
scalaVersion := "2.11.11"

lazy val root = project.in(file("."))
  .settings(Common.settings)
  .settings(publishArtifact := false)
  .aggregate(
    common,
    manager,
    gateway
  )


lazy val common = project.in(file("common"))
  .settings(Common.settings)
  .settings(publishArtifact := false)
  .settings(exportJars := true)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)

lazy val gateway = project.in(file("gateway"))
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.commonDependencies)
  .dependsOn(common)

lazy val codegen = project.in(file("codegen"))
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.codegenDependencies)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = false) -> (sourceManaged in Compile).value
    ),
    libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
  )

lazy val manager = project.in(file("manager"))
  .settings(Common.settings)
  .settings(libraryDependencies ++= Dependencies.hydroServingManagerDependencies)
  .dependsOn(codegen, common)
