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
  val PathPrefix = MyUtil.pathFile(Helpers.property("fairplay_script")) + "."

  /**
   * Get directory containing experiment-specific data files
   * @return  string of directory name
   */
  def getPrefix() = {
    val scriptFile = Helpers.property("fairplay_script")
    new File(Helpers.property("data_directory"), scriptFile.substring(scriptFile.lastIndexOf("/") + 1)).toString + "."
  }


  // Return: (AliceOutput, BobOutput)
  def readOutputs() = {
    ( MyUtil.readResult(PathPrefix + "Alice.output").filter(_ != null).asInstanceOf[Array[BigInteger]],
      MyUtil.readResult(PathPrefix + "Bob.output").filter(_ != null).asInstanceOf[Array[BigInteger]] )
  }


  /**
   * For test only. Generates .input files for Fairplay
   * @param xValue  the x value as in ln(x)
   * @return  no return. Will create related files
   */
  def prepareInputs(xValue: BigInteger) = {
    val writers = Array("Bob", "Alice").map(a => new PrintWriter(new File(PathPrefix + a + ".input")))
    val shareRand = BigInteger.valueOf(3)  //TODO use real rand like: rnd.nextInt(rndRange)
    writers(0).println(shareRand)
    writers(0).println(2)  //TODO (rnd.nextInt(rndRange))
    writers(0).println(5)  //TODO (rnd.nextInt(rndRange))

    writers(1).println(xValue.subtract(shareRand))
    writers.map(a => a.close())
  }


  /**
   * Create data directory for current experiment
   * @return  no return. Will create related directory
   */
  def createDataDir() = {
    val dataDir = Helpers.property("data_directory")
    val dataHandler = new File(dataDir)
    if (!dataHandler.exists()) dataHandler.mkdir()

    dataDir
  }


  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    createDataDir()

    Mediator.compile()  //compile once

    val dataDir = getPrefix()
    // track run time
    val timeWriter = new PrintWriter(new File(dataDir + "time"))
    // store results from multiple experiments
    val resultWriter = new PrintWriter(new File(dataDir + "lnx.csv"))

    resultWriter.println(""""accuracy: ","1E-6"""")
    resultWriter.println(""""input x","secure ln(x)","actual ln(x)","absolute error","relative error"""")

    // Run multiple experiments
    var count = 0  //experiment index
    val (startValue, endValue) = (Helpers.property("start_value").toInt, Helpers.property("end_value").toInt)
    for (xValue <- startValue to endValue) {
      println(">> Experiment with x = " + xValue)
      count += 1

      prepareInputs(BigInteger.valueOf(xValue))

      // Run Bob and Alice
      AutomatedTest.main(Array())

      val (_, bobOutputs) = readOutputs()

      val computedResult = Mediator.actualLn(bobOutputs(0), bobOutputs(1), 10).doubleValue()
      val expectedResult = math.log(xValue)

      resultWriter.println(xValue + "," + computedResult + "," + expectedResult + "," +
        (computedResult - expectedResult) + "," + (computedResult - expectedResult)/expectedResult)

      if (xValue % Helpers.property("flush_per_iterations").toInt == 0 || xValue.equals(endValue)) {
        resultWriter.flush()
        val inSeconds = (System.currentTimeMillis() - startedAt) / 1000
        timeWriter.println("Processed till value: " + xValue + "\nTotal time: " + inSeconds + " seconds.\n")
        timeWriter.flush()
      }
    }

    resultWriter.close()
    timeWriter.close()


    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000 + " seconds.")
  }
}