name := "RasterdataEndlessSource"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "com.typesafe.play" % "play-json_2.12" % "2.6.3",
  "com.typesafe" % "config" % "1.3.1"
)

assemblyJarName in assembly := "rasterdata-endless-source.jar"