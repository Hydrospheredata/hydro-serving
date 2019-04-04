logLevel := Level.Info

resolvers += "Flyway" at "https://davidmweber.github.io/flyway-sbt.repo"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.5")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.6.0")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.5.3")


libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.8.0",
  "org.flywaydb" % "flyway-core" % "4.2.0",
  "com.thesamet.scalapb" %% "compilerplugin" % "0.7.4"
)