name := "spiders-from-mars"
version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.26",
  "org.slf4j" % "slf4j-simple" % "1.7.26",

  "org.asynchttpclient" % "async-http-client" % "2.8.1",
  "org.jsoup" % "jsoup" % "1.12.1",

  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",

  "com.typesafe.slick" %% "slick" % "3.3.0",
  "mysql" % "mysql-connector-java" % "5.1.34",
  
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  ws, // Web client library, coming from the Play Framework
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test

)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

test in assembly := {}
