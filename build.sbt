name := "arbre"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "com.assembla.scala-incubator" %% "graph-core" % "1.9.0",
  "com.assembla.scala-incubator" %% "graph-dot" % "1.9.0"
)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
