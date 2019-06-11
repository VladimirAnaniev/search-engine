name := "spiders-from-mars"
version := "0.1"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.26",
  "org.slf4j" % "slf4j-simple" % "1.7.26",

  "org.asynchttpclient" % "async-http-client" % "2.8.1",
  "org.jsoup" % "jsoup" % "1.12.1",

  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

test in assembly := {}
