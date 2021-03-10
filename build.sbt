ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.rhysmills"
ThisBuild / organizationName := "rhysmills"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "comment-lambda",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
      "com.amazonaws" % "aws-lambda-java-events" % "3.7.0",
      "org.scalatest" %% "scalatest" % "3.2.2" % Test,
    ),
    //Native packager
    topLevelDirectory in Universal := None,
    packageName in Universal := "comment-lambda",
  )

lazy val devServer = (project in file("devServer"))
  .settings(
    name := "devServer",
    libraryDependencies ++= Seq(
      "io.javalin" % "javalin" % "3.11.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    ),
    fork in run := true, // These mean web server will write to console, and ctrl c will kill the web server, not sbt
    connectInput in run := true,
    outputStrategy := Some(StdoutOutput),
  )
  .dependsOn(root)