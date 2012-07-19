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
import java.math.BigInteger

object Experiment {
  val PathPrefix = "run/progs/Sub.txt."

  def readOutputs() = {
    val aliceOutput = MyUtil.readResult(PathPrefix + "Alice.output").filter(_ != null).asInstanceOf[Array[BigInteger]]
    val bobOutput = MyUtil.readResult(PathPrefix + "Bob.output").filter(_ != null).asInstanceOf[Array[BigInteger]]

    (aliceOutput, bobOutput)
  }


  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    val rnd = new Random(System.currentTimeMillis())

    val dataDir = "data" + PathPrefix.substring(PathPrefix.lastIndexOf("/"))
    val timeWriter = new PrintWriter(new File(dataDir + "time"))
    // Run multiple experiments
    val resultWriter = new PrintWriter(new File(dataDir + "lnx.csv"))

    resultWriter.println(""""accuracy: ","1E-6"""")
    resultWriter.println(""""input x","secure ln(x)","actual ln(x)","absolute error","relative error"""")

    var count = 0
    for (xValue <- 1 to 80) {
      println(">> Experiment with x = " + xValue)
      count += 1
      // Generate test cases for Bob and Alice
      val writers = Array("Bob", "Alice").map(a => new PrintWriter(new File(MyUtil.pathFile("progs/Sub.txt." + a + ".input"))))
      val shareRand = 3  //rnd.nextInt(rndRange)
      writers(0).println(shareRand)
      writers(0).println(2)  //(rnd.nextInt(rndRange))
      writers(0).println(5)  //(rnd.nextInt(rndRange))

      writers(1).println(xValue - shareRand)
      writers.map(a => a.close())

      // Run Bob and Alice
      AutomatedTest.main(Array())

      val (_, bobOutputs) = readOutputs()

      val computedResult = Mediator.secureLn(bobOutputs(0), bobOutputs(1), 10).doubleValue()
      val expectedResult = math.log(xValue)

      resultWriter.println(xValue + "," + computedResult + "," + expectedResult + "," +
        (computedResult - expectedResult) + "," + (computedResult - expectedResult)/expectedResult)

      if (xValue % 10 == 0) {
        resultWriter.flush()
        val inSeconds = (System.currentTimeMillis() - startedAt) / 1000
        timeWriter.println("\r# of values tested: " + count + "\nTotal time: " + inSeconds + " seconds.\nAverage time per value: " + inSeconds.toDouble / count + " seconds.")
      }
    }


    resultWriter.close()
    timeWriter.close()


    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000 + " seconds.")
  }
}