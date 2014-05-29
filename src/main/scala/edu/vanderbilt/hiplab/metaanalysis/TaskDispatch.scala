package edu.vanderbilt.hiplab.metaanalysis

import java.util.Properties
import java.io.{File, FileOutputStream, FileInputStream}

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 10/26/12
 */
object TaskDispatch {
  // To customize: data_directory, start_n, end_n, socket_port
  def customizeConf(experimentName: String) = {
    println("> To customize conf.properties...")
    val end_n = Helpers.property("end_n").toInt
    val start_n = Helpers.property("start_n").toInt
    val casesPerProcess = (end_n + 1 - start_n) / Helpers.property("total_instances").toInt

    for (indx <- 0 to Helpers.property("total_instances").toInt - 1) {
      val fis = new FileInputStream("conf.properties")

      val prop = new Properties()
      prop.load(fis)
      fis.close()

      val currentStartN = indx * casesPerProcess + start_n

      prop.setProperty("data_directory", "data")
      prop.setProperty("start_n", currentStartN.toString)
      prop.setProperty("end_n", (currentStartN + casesPerProcess - 1).toString)
      prop.setProperty("socket_port", (3497 + indx).toString)

      val fos = new FileOutputStream(new File("experiment/" + experimentName, "process" + indx + "/conf.properties").toString)
      prop.store(fos, "Process #" + indx)
      fos.close()
    }

    println("  END of customizing properties!")
  }

  def main(args: Array[String]) = {
    println("Preparing files using Bash...")
    //Runtime.getRuntime.exec("/bin/bash prepare_experiment.sh blabla2 " + Helpers.property("total_instances"))
    println("End of bash!")

    customizeConf("blabla2")
  }
}
