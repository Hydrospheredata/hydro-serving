lazy val codegen = project.in(file("."))
  .settings(Settings.all)
  .settings(
    exportJars := true,
    libraryDependencies ++= Dependencies.codegenDependencies
  )
