name := "kafka-integration-patterns"

import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import Common._

inThisBuild(
  Seq(
    version in ThisBuild := "1.0",
    organization in ThisBuild := "com.paro",
      scalaVersion in ThisBuild := "2.12.6",
    parallelExecution := false,
    scalafmtOnCompile := true
  )
)


lazy val global = project
  .in(file("."))
  .aggregate(
    models,
    csvToKafka,
    csvToJson,
    csvToES,
    kafkaToES
  )


lazy val models = project.
  in(file("models")).
  settings(commonSettings)
  .settings(libraryDependencies ++= circeDependencies)

lazy val csvToKafka = project.
  in(file("cvs-to-kafka"))
  .enablePlugins(ProjectAppPackager)
  .settings(commonSettings)
  .settings(
    name:="csv-to-kafka"
  )
  .settings(libraryDependencies ++= Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "0.20"
  ) ++ akkaStreamDependencies ++ kamonDependencies
  ).dependsOn(models)

lazy val csvToJson = project.
  in(file("cvs-to-json"))
  .enablePlugins(ProjectAppPackager)
  .settings(commonSettings)
  .settings(
    name:="csv-to-json"
  )
  .settings(libraryDependencies ++= Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "0.20"
  ) ++ akkaStreamDependencies ++ kamonDependencies
  ).dependsOn(models)


lazy val kafkaToES = project
  .in(file("kafka-to-es"))
  .enablePlugins(ProjectAppPackager)
  .settings(commonSettings)
  .settings(
    name:="kafka-to-es"
  )
  .settings(libraryDependencies ++= Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % "0.20"
  ) ++ akkaStreamDependencies ++ kamonDependencies
  ).dependsOn(models)

lazy val csvToES = project
  .in(file("csv-to-es"))
  .enablePlugins(ProjectAppPackager)
  .settings(commonSettings)
  .settings(
    name:="csv-to-es"
  )
  .settings(libraryDependencies ++= Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "0.20",
    "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % "0.20"
  ) ++ akkaStreamDependencies ++ kamonDependencies
  ).dependsOn(models)
