import sbt.Keys._
import sbt._

object Common {
  val appName="kafka-etl"

  val circeVersion = "0.9.3"

  lazy val compilerOptions = Seq(
    "-unchecked",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-deprecation",
    "-encoding",
    "utf8"
  )


  lazy val commonSettings = Seq(
    scalacOptions ++= compilerOptions,
    scalaVersion := "2.12.6",
    parallelExecution in Test := false,
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
    )
  )


  lazy val kamonDependencies = Seq(
    "io.kamon" %% "kamon-core" % "1.1.3",
    "io.kamon" %% "kamon-system-metrics" % "1.0.0",
    // Optional Dependencies
    "io.kamon" %% "kamon-prometheus" % "1.1.1"
    //"io.kamon" %% "kamon-zipkin" % "1.0.0"
  )

  lazy val akkaStreamDependencies = Seq(
    "com.typesafe.akka" %% "akka-slf4j" % "2.5.13",
    "com.typesafe.akka" %% "akka-stream" % "2.5.13",
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.22",
    "org.slf4j" % "slf4j-simple" % "1.7.25"

  )


  lazy val circeDependencies = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-java8",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)

}
