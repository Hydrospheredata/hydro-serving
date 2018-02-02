logLevel := Level.Info

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")

resolvers += "Flyway" at "https://flywaydb.org/repo"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.2.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.9.0")

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.8.0"
)