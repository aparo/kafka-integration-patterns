// Server side plugins
addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.9.1")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.3.4")
addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj"         % "0.11.0")
addSbtPlugin("com.tapad"         % "sbt-docker-compose"  % "1.0.28")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.1")

// Scala style and formatting for this plugins code
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")


addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.6.0-RC3")
