import play.Project._

name := """cloud-plex"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.2", 
  "org.webjars" % "bootstrap" % "2.3.1",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1")

playJavaSettings
