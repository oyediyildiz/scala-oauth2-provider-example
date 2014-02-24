name := "scala-oauth2-provider-example"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.typesafe.play" %% "play-slick" % "0.6.0.1",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.3.0"
)     

play.Project.playScalaSettings
