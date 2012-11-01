package main

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 10/26/12
 */
object TaskDispatch {
  def main(args: Array[String]) = {
    println("Preparing files using Bash...")
    Runtime.getRuntime.exec("/bin/bash prepare_experiment.sh blabla")
    println("End of bash!")

  }
}
