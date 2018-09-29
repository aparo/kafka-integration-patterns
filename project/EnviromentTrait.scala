// Copyright: 2018 https://www.nttdata.com
// License: http://www.nttdata.com/licenses/LICENSE-2.0

package com.nttdata

import sbt.Def
import spray.revolver.RevolverPlugin.autoImport.Revolver

object EnviromentGlobal {
  def appName: String=
    if (sys.env.contains("APP_NAME")) sys.env("APP_NAME") else "nttdata"

}
