import PlayKeys._

name := """cloud-plex"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "jquery" % "2.1.1",
  "org.webjars" % "bootstrap" % "3.1.1-2",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
  "ws.securesocial" %% "securesocial" % "master-SNAPSHOT",
  jdbc,
  javaEbean,
  cache,
  "mysql" % "mysql-connector-java" % "5.1.27")

lazy val root = (project in file(".")).enablePlugins(PlayJava)
