import sbt._
import sbt.Keys._
import com.lightbend.sbt.SbtAspectj.autoImport.Aspectj
import com.lightbend.sbt.AspectjKeys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.{JavaAppPackaging, JavaServerAppPackaging}
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin

trait ProjectPackagerKeys {
  val extraJvmParams = settingKey[Seq[String]]("Extra JVM parameters")
}
object ProjectPackagerKeys extends ProjectPackagerKeys

abstract class ProjectPackager extends AutoPlugin {

  protected val linuxHomeLocation = s"/opt/${Common.appName}"

  override def requires: Plugins = DockerPlugin

  protected val defaultJvmParams = Seq(
//    "-Dconfig.file=${app_home}/../conf/application.conf",
//    "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml"
  )

  protected lazy val defaultPackagingSettings: Seq[Def.Setting[_]] = Seq(
    //ProjectPackagerKeys.extraJvmParams := defaultJvmParams,
    bashScriptExtraDefines ++= ProjectPackagerKeys.extraJvmParams.value.map(p => s"""addJava "$p""""),
    maintainer in Docker := "Alberto Maria Angelo Paro",
    dockerRepository := Some(Common.appName),
    dockerUpdateLatest := true,
    dockerExposedVolumes := Seq(
      s"$linuxHomeLocation/conf",
      "/opt/data"
    ),
    defaultLinuxInstallLocation in Docker := linuxHomeLocation,
    dockerCommands ++= Seq(
      Cmd("ENV", "APPLICATION_HOME", linuxHomeLocation)
    ),
    packageName := name.value,
    packageName in Universal := s"${moduleName.value}-${version.value}",
    executableScriptName := moduleName.value
  )

}

object ProjectAppPackager extends ProjectPackager {

  import ApplicationKeys._

  val autoImport = ProjectPackagerKeys

  import autoImport._

  override def requires: Plugins = super.requires && JavaAppPackaging  && ProjectApp

  override lazy val projectSettings = defaultPackagingSettings++ Seq(
    mappings in Universal ++= Seq(
      (aspectjWeaver in Aspectj).value.get -> "bin/aspectjweaver.jar",
      sigarLoader.value -> "bin/sigar-loader.jar"
    ),
    extraJvmParams := defaultJvmParams ++ Seq(
      "-javaagent:${app_home}/aspectjweaver.jar",
      "-javaagent:${app_home}/sigar-loader.jar"
    ),
    dockerExposedVolumes ++= Seq(
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/local"
    )
  )

}

object ProjectServerPackage extends ProjectPackager {

  import ApplicationKeys._

  val autoImport = ProjectPackagerKeys

  import autoImport._

  override def requires: Plugins = super.requires && JavaServerAppPackaging && ProjectApp

  override lazy val projectSettings = defaultPackagingSettings ++ Seq(
    mappings in Universal ++= Seq(
      (aspectjWeaver in Aspectj).value.get -> "bin/aspectjweaver.jar",
      sigarLoader.value -> "bin/sigar-loader.jar"
    ),
    extraJvmParams := defaultJvmParams ++ Seq(
      "-javaagent:${app_home}/aspectjweaver.jar",
      "-javaagent:${app_home}/sigar-loader.jar"
    ),
    dockerExposedVolumes ++= Seq(
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/local"
    )
  )
}
