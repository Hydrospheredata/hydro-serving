logLevel := Level.Info

// Workaround for sbt 1.0.0
resolvers += "Flyway" at "https://davidmweber.github.io/flyway-sbt.repo"
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.10.0",
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
)
