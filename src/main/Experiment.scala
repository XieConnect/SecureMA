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
import paillierp.Paillier
import paillierp.key.KeyGen

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

  /**
   * Generate README in experiment data directory for better description of data
   */
  def generateReadme() = {
    val writer = new PrintWriter(new File(getPrefix() + "README"))
    writer.println("Start n: " + Helpers.property("start_n") + "\n" +
      "End n: " + Helpers.property("end_n") + "\n" +
      "Note: " + Helpers.property("readme")
    )
    writer.close()
  }


  /**
   * Generate test cases given the start and end values of N (from config file)
   * @return  array of all candidate values
   */
  def generateTestCases(startN: Int, endN: Int) = {
    (startN to endN).map { n =>
      val tmp = math.pow(2, n - 2).toInt
      (4 * tmp + 3 * n - 8) to (tmp * 5)
    }.flatten.distinct
  }

  /**
   * Split large list of test cases into smaller shares and distribute computation
   * TODO remove dependency on global conf param
   * @param startN minimum n as in 2^n
   * @param endN maximum n as in 2^n
   * @return collection of test cases for this running instance
   */
  def perInstanceCases(startN: Int, endN: Int) = {
    val cases = generateTestCases(startN, endN)
    val perInstance = cases.length / Helpers.property("total_instances").toInt
    val currentIndex = Helpers.property("current_instance").toInt * perInstance
    cases.slice(currentIndex, currentIndex + perInstance)
  }

  /**
   * Selectively generate test cases to find optimal turning points
   * @param startN minimum n as in 2^n
   * @param endN maximum n as in 2^n
   * @return collection of semi-optimal test cases
   */
  def pointsAroundTurning(startN: Int, endN: Int) = {
    (startN to endN).map { n =>
      val tmp = math.pow(2, n - 2).toInt * 5
      tmp - 1 to tmp + 5
    }.flatten.distinct
  }

  // ln(x) accuracy will be a little lower than (less accurate) the value calculated below, given number of Taylor expansions
  // Refer to Lindell's paper for more details
  def getAccuracy() = {
    val powerExponent = Helpers.property("max_exponent_n").toInt
    4 / math.log(2) / (math.pow(2, powerExponent) * powerExponent)
  }

  /**
   * Experiment with secure ln(x) computation
   * TODO re-use lnWrapper()
   * @param startedAt  when the experiment starts
   */
  def runLn(startedAt: Long) = {
    createDataDir()

    val dataDir = getPrefix()
    // track run time
    val timeWriter = new PrintWriter(new File(dataDir + "time.csv"))
    // store results from multiple experiments
    val resultWriter = new PrintWriter(new File(dataDir + "lnx.csv"))

    resultWriter.println("\"less accurate than: \"," + getAccuracy())
    resultWriter.println(""""input x","secure ln(x)","actual ln(x)","absolute error","relative error"""")

    // Run multiple experiments
    var count = 0  //experiment index
    val flushPerIterations = Helpers.property("flush_per_iterations").toInt
    val List(startN, endN) = List("start_n", "end_n").map(i => Helpers.property(i).toInt)

    val testCases = perInstanceCases(startN, endN)  //pointsAroundTurning(startN, endN)  //generateTestCases()

    val endValue = testCases.last
    println("> " + testCases.length + " test cases to process [" + testCases.head + "..." + endValue + "]...")

    timeWriter.println(""""start value:",""" + testCases.head + ""","end value:",""" + endValue)
    timeWriter.println(""""aggregate number of values","aggregate seconds"""")

    for (xValue <- testCases) {
      println("> x = " + xValue)
      count += 1

      prepareInputs(BigInteger.valueOf(xValue))

      // Run Bob and Alice
      var bobArgs = Array[String]()
      if (count == 1) bobArgs :+= "init"  //compile and generate keys only once

      AutomatedTest.main(bobArgs)

      val (_, bobOutputs) = readOutputs()

      val computedResult = Mediator.actualLn(bobOutputs(0), bobOutputs(1), 10).doubleValue()


      val expectedResult = math.log(xValue)

      resultWriter.println(xValue + "," + computedResult + "," + expectedResult + "," +
        (computedResult - expectedResult) + "," + (computedResult - expectedResult)/expectedResult)

      if (count % flushPerIterations == 0 || xValue.equals(endValue)) {
        resultWriter.flush()
        val inSeconds = (System.currentTimeMillis() - startedAt) / 1000
        //timeWriter.println("Processed " + count + " values till now.\nAggregate time: " + inSeconds + " seconds.\n")
        timeWriter.println(count + "," + inSeconds)
        timeWriter.flush()
      }
    }

    resultWriter.close()
    timeWriter.close()

    generateReadme()
  }

  /**
   * Input x, compute ln(x) encryption result
   * @param xValue x as in ln(x)
   * @param toInit whether to generate keys/compile Fairplay script or not
   * @return encryption of ln(x) result
   */
  def lnWrapper(xValue: BigInteger, toInit: Boolean = false): BigInteger = {
    prepareInputs(xValue)

    // Run Bob and Alice
    var bobArgs = Array[String]()
    if (toInit) bobArgs :+= "init"  //compile and generate keys only once

    AutomatedTest.main(bobArgs)

    val (_, bobOutputs) = readOutputs()

    Mediator.secureLn(bobOutputs(0), bobOutputs(1))
  }

  /**
   * Given numerator, denominator, compute actual division result
   * @param numerator numerator in plain value
   * @param denominator denominator in plain value
   * @param toInit whether or not to generate keys/compile Fairplay
   */
  //TODO remove cheat about determining result sign
  def runDivision(numerator: BigInteger, denominator: BigInteger, toInit: Boolean = false) = {
    val numeratorLn = lnWrapper(numerator, toInit)
    val denominatorLn = lnWrapper(denominator, false)
    val someone = new Paillier(Helpers.getPublicKey())
    val fieldN = KeyGen.PaillierThresholdKeyLoad(new File(Helpers.property("data_directory"), Helpers.property("private_keys")).toString)(0).getN
    val diff = someone.add(numeratorLn, someone.multiply(denominatorLn, -1))

    val negative = if (numerator.compareTo(denominator) >= 0) false else true
    println(math.exp(Mediator.decryptLn(diff, 10, negative)))
  }

  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()

    runLn(startedAt)

    //runDivision(new BigInteger("4000000"), new BigInteger("400"), toInit = false)

    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000 + " seconds.")
  }
}