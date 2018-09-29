#!/usr/bin/env amm

import ammonite.ops._
import ammonite.ops.ImplicitWd._
import $ivy.`com.lihaoyi::ujson:0.6.5`
import ujson.Js

case class Service(containerId:String, image:String, command:String, created:String, status:String, ports:String, names:String) {
  lazy val inspect={
    val lines= %%('docker, s"inspect", containerId)
    ujson.read(lines.out.lines.toList.mkString(""))(0)
  }

  def ipAddress:String=inspect("NetworkSettings")("Networks")("dockprom_monitor-net")("IPAddress").toString.replace("\"", "")

}

object Service {
  def retrieve():List[Service]={
    val res = %%('docker, "ps")
    val lines=res.out.lines //.tail.map(t => t.split("\\s").head -> t.split("\\s").drop(6).dropWhile(_.isEmpty).head)
    val header = lines.head
    val containerIdPos=0
    val imagePos=header.indexOf("IMAGE")
    val commandPos=header.indexOf("COMMAND")
    val createdPos=header.indexOf("CREATED")
    val statusPos=header.indexOf("STATUS")
    val portsPos=header.indexOf("PORTS")
    val namesPos=header.indexOf("NAMES")
//    println(s"$containerIdPos $imagePos $commandPos $createdPos $portsPos $statusPos $namesPos")
    lines.tail.toList.map{
      line:String =>
        Service(
          line.substring(0,imagePos).trim,
          line.substring(imagePos, commandPos).trim,
          line.substring(commandPos,createdPos ).trim,
          line.substring(createdPos, statusPos).trim,
          line.substring(statusPos, portsPos).trim,
          line.substring(portsPos, namesPos).trim,
      line.substring(namesPos).trim)
    }//.sortedBy(_.names)
  }
}

val services=Service.retrieve()

services.foreach{
  service =>
    println(s"${service.names}: ${service.ipAddress}")
//    val lines = %%('docker, s"inspect",service._1)
//    val ip = lines.out.lines.filter(_.contains("IPAddress")).filter(_.contains("172.19.0")).head.split(':')(1)
//    println(s"Ip ${service._2} $ip")

}
