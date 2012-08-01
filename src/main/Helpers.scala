package main

import java.io.FileInputStream
import java.util.Properties

/**
 * @description Helper methods for project
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/30/12
 */

object Helpers {
  val MyProperties = new Properties()

  def property(key: String) = {
    MyProperties.load(new FileInputStream("conf.properties"))
    MyProperties.getProperty(key)
  }

}
