import org.sbtidea.SbtIdeaPlugin._
import sbt.Keys._
import sbt._

object GarudaBuild extends sbt.Build {

  val sharedDeps = Seq(
    "io.netty" % "netty-all" % "4.0.18.Final",
    "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
    "ch.qos.logback" % "logback-core" % "1.1.1",
    "ch.qos.logback" % "logback-classic" % "1.1.1",
    "org.specs2" %% "specs2" % "2.4.6" % "test"
  )

  val metricsScalaDeps = Seq(
    "com.codahale.metrics" % "metrics-core" % "3.0.2",
    "com.codahale.metrics" % "metrics-healthchecks" % "3.0.2",
    "junit" % "junit" % "4.11" % "test",
    "org.scalatest" %% "scalatest" % "1.9.1" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test"
  )

  val serverDeps = Seq(
    "org.javassist" % "javassist" % "3.18.1-GA",
    "com.cloudhopper" % "ch-smpp" % "5.0.3" % "test"
  )

  val sharedSettings = Seq(
    organization := "io.garuda",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.10.3",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xlint",
      "-target:jvm-1.7",
      "-encoding", "UTF-8"
    ),
    scalacOptions in(Compile, doc) ++= Seq(
      "-groups",
      "-implicits"
    ),
    javacOptions ++= Seq(
      "-Xmx1024m",
      "-Xms256m",
      "-Xss10m"
    ),
    javaOptions ++= Seq(
      "-Xms256m",
      "-Xmx1536m",
      "-Djava.awt.headless=true"
    ),
    libraryDependencies ++= sharedDeps,
    resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots",
    resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    initialCommands := "import io.garuda._"
  )

  lazy val root = Project(
    id = "garuda",
    base = file("."),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      ideaExcludeFolders := ".idea" :: ".idea_modules" :: Nil
    )
  ) aggregate(codec, common, metricsScala, windowing, server)

  lazy val metricsScala = Project(
    id = "metrics-scala",
    base = file("metrics-scala"),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      name := "metrics-scala",
      libraryDependencies ++= metricsScalaDeps
    )
  )

  lazy val common = Project(
    id = "common",
    base = file("common"),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      name := "common"
    )
  ) dependsOn(metricsScala, codec)

  lazy val codec = Project(
    id = "smpp-codec",
    base = file("smpp-codec"),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      name := "smpp-codec"
    )
  ) dependsOn metricsScala


  lazy val windowing = Project(
    id = "windowing",
    base = file("windowing"),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      name := "windowing"
    )
  ) dependsOn(metricsScala, codec, common)

  lazy val server = Project(
    id = "server",
    base = file("server"),
    settings = Project.defaultSettings ++ sharedSettings ++ Seq(
      name := "server",
      libraryDependencies ++= serverDeps,
      libraryDependencies ++= metricsScalaDeps
    )
  ) dependsOn(metricsScala, codec, common, windowing)
}
