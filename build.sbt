name := """twitterstream"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.apache.spark" % "spark-core_2.11" % "2.1.0",
  "com.datastax.spark" % "spark-cassandra-connector_2.11" % "2.0.1",
  "org.apache.spark" % "spark-sql_2.11" % "2.1.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.twitter4j" % "twitter4j-core" % "4.0.4",
  "org.twitter4j" % "twitter4j-async" % "4.0.4",
  "org.twitter4j" % "twitter4j-stream" % "4.0.4",
  "org.twitter4j" % "twitter4j-media-support" % "4.0.4",
  "com.typesafe.akka" % "akka-stream_2.11" % "2.5.1",
  "com.typesafe.play" % "play-streams_2.11" % "2.5.14",
  "com.typesafe.play" % "play_2.11" % "2.5.14",
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.2.0"

)
dependencyOverrides ++= Set("com.fasterxml.jackson.core" % "jackson-databind" % "2.6.0")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"


//fork in run := true