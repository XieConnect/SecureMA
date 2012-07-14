package main

/**
 * @description Run whole protocol on test data
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/13/12
 */

import java.util.Random
import java.io.{PrintWriter, File}
import SFE.BOAL.MyUtil
import test.AutomatedTest

object Experiment {
  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    val rnd = new Random(System.currentTimeMillis())

    // Run multiple experiments
    for (x <- 2 to 3) {
      println(">> Experiment with x = " + x)
      // Generate test cases for Bob and Alice
      val writers = Array("Bob", "Alice").map(a => new PrintWriter(new File(MyUtil.pathFile("progs/Sub.txt." + a + ".input"))))
      val shareRand = rnd.nextLong()
      writers(0).println(shareRand)
      writers(0).println(rnd.nextLong())
      writers(0).println(rnd.nextLong())

      writers(1).println(x - shareRand)
      writers.map(a => a.close())

      // Run Bob and Alice
      AutomatedTest.main(Array())
    }

    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000 + " seconds.")
  }
}