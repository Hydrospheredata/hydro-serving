logLevel := Level.Info

resolvers += "Flyway" at "https://flywaydb.org/repo"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.8.0"
)

