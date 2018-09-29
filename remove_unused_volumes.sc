#!/usr/bin/env amm

import ammonite.ops._
import ammonite.ops.ImplicitWd._

val res = %%('docker, "volume", "ls")
val services=res.out.lines.tail.map(t => t.split("\\s").head -> t.split("\\s").filterNot(_.isEmpty).drop(1).head)

services.foreach{
  service =>
    println(s"Removing volume ${service}")
    try{
      %%('docker, "volume", "rm", service._2)
    }catch {
      case ex:Throwable =>
    }
}
