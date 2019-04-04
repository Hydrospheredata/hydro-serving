lazy val codegen = project.in(file("."))
  .settings(Common.settings)
  .settings(
    exportJars := true,
    libraryDependencies ++= Dependencies.codegenDependencies
  )
