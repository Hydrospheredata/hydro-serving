import sbt._

object Dependencies {
  val akkaVersion = "2.5.3"
  val akkaHttpVersion = "10.0.9"
  val hadoopVersion = "2.8.0"
  val log4j2Version = "2.8.2"
  val slickVersion = "3.2.1"
  val postgresqlVersion = "42.1.3"
  val scalaTestVersion = "3.0.3"
  val slickPgVersion = "0.15.4"
  val awsSdkVersion = "1.11.184"

  lazy val hdfsDependencies = Seq(
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion
  )

  lazy val akkaDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  )

  lazy val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    //"com.typesafe.akka" %% "akka-http-jackson" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
    "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.9.2" exclude("javax.ws.rs", "jsr311-api"),
    "ch.megard" %% "akka-http-cors" % "0.2.1"
  )

  lazy val testDependencies = Seq(
    "org.mockito" % "mockito-all" % "1.10.19" % "test,it",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test,it",
    "com.dimafeng" %% "testcontainers-scala" % "0.7.0" % "test,it",
    "org.scalactic" %% "scalactic" % scalaTestVersion % "test,it",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test,it",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test,it",
    "com.amazonaws" % "aws-java-sdk-test-utils" % "1.11.174" % "test,it",
    "io.findify" %% "s3mock" % "0.2.3" % "test,it",
    "io.findify" %% "sqsmock" % "0.3.2" % "test,it"

  )

  lazy val logDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-api" % log4j2Version,
    "org.apache.logging.log4j" % "log4j-core" % log4j2Version,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2Version,
    "org.apache.logging.log4j" %% "log4j-api-scala" % log4j2Version
  )

  lazy val commonDependencies = akkaDependencies
    .union(akkaHttpDependencies)
    .union(logDependencies)

  lazy val streamingKafkaDependencies = akkaDependencies
    .union(commonDependencies)
    .union(Seq(
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.17"
    ))

  lazy val codegenDependencies = commonDependencies
    .union(Seq(
      "org.postgresql" % "postgresql" % postgresqlVersion,
      "com.github.tminglei" %% "slick-pg" % "0.15.1",
      "com.typesafe.slick" %% "slick-codegen" % slickVersion,
      "com.github.tminglei" %% "slick-pg_spray-json" % slickPgVersion
    ))

  lazy val hydroServingManagerDependencies = commonDependencies
    .union(akkaHttpDependencies)
    .union(testDependencies)
    .union(Seq(
      "com.amazonaws" % "aws-java-sdk-ecs" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-iam" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-ecr" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-sqs" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion,
      "org.postgresql" % "postgresql" % postgresqlVersion,
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
      "com.zaxxer" % "HikariCP" % "2.6.3",
      "com.github.tminglei" %% "slick-pg" % slickPgVersion,
      "com.github.tminglei" %% "slick-pg_spray-json" % slickPgVersion,
      "org.flywaydb" % "flyway-core" % "4.2.0",
      "com.spotify" % "docker-client" % "8.8.0" exclude("ch.qos.logback", "logback-classic"),
      "com.google.guava" % "guava" % "22.0",
      "org.tensorflow" % "proto" % "1.2.1",
      "org.tensorflow" % "proto" % "1.2.1"
    ))
}
