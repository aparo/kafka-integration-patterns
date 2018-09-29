#!/usr/bin/env amm

import ammonite.ops._
import ammonite.ops.ImplicitWd._

val res = %%('docker, "ps", "-a")
val services=res.out.lines.tail.filter(_.contains("Exited")).map(t => t.split("\\s").head -> t.split("\\s").drop(1).dropWhile(_.isEmpty).head)
services.foreach{
  service =>
    println(s"Starting ${service._2}")
    %%('docker, s"start",service._1)

}
