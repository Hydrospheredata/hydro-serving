logLevel := Level.Info

resolvers += "Flyway" at "https://flywaydb.org/repo"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.9.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.8.0",
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
)
