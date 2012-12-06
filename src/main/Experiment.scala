package main

/**
 * @description Run whole protocol on test data
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/13/12
 */

import java.util.Random
import java.io.{FileInputStream, FileOutputStream, PrintWriter, File}
import SFE.BOAL.MyUtil
import test.AutomatedTest
import java.math.BigInteger
import paillierp.Paillier
import paillierp.key.KeyGen

object Experiment {
  val PathPrefix = MyUtil.pathFile(Helpers.property("fairplay_script")) + "."

  /**
   * Directory path containing data for current experiment
   * @return  directory name string
   */
  def getPrefix(): String = {
    val scriptFile = Helpers.property("fairplay_script")
    new File(Helpers.property("data_directory"), scriptFile.substring(scriptFile.lastIndexOf("/") + 1)).toString + "."
  }


  // Return: (AliceOutput, BobOutput)
  def readOutputs(): Tuple2[Array[BigInteger], Array[BigInteger]] = {
    ( MyUtil.readResult(PathPrefix + "Alice.output").filter(_ != null).asInstanceOf[Array[BigInteger]],
      MyUtil.readResult(PathPrefix + "Bob.output").filter(_ != null).asInstanceOf[Array[BigInteger]] )
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

  def generateAllCases(startN: Int, endN: Int) = {
    startN to endN
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
      //val tmp = math.pow(2, n - 2).toInt * 5
      //tmp - 1 to tmp + 5
      math.pow(2, n).toInt to (math.pow(2, n-1) * 3).toInt
    }.flatten.distinct
  }

  // ln(x) accuracy will be a little lower than (less accurate) the value calculated below, given number of Taylor expansions
  // Refer to Lindell's paper for more details
  def getAccuracy() = {
    val powerExponent = Helpers.property("max_exponent_n").toInt
    4 / math.log(2) / (math.pow(2, powerExponent) * powerExponent)
  }

  def copyFiles() = {
    for (a <- Array("conf.properties", MyUtil.pathFile(Helpers.property("fairplay_script")))) {
      val filename = a.substring(a.lastIndexOf("/") + 1)
      new FileOutputStream(Helpers.property("data_directory") + "/" + filename) getChannel() transferFrom(
        new FileInputStream(a) getChannel(), 0, Long.MaxValue
      )
    }
  }

  /**
   * Experiment with secure ln(x) computation
   * TODO re-use lnWrapper()
   * @param startedAt  when the experiment starts
   */
  def runLn(startedAt: Long) = {
    val dataDir = getPrefix()
    // track runtime
    val timeWriter = new PrintWriter(new File(dataDir + "time.csv"))
    // store results from multiple experiments
    val resultWriter = new PrintWriter(new File(dataDir + "lnx.csv"))

    resultWriter.println("\"less accurate than: \"," + getAccuracy())
    resultWriter.println(""""input x","secure ln(x)","actual ln(x)","absolute error","relative error"""")

    var count = 0  //experiment count
    val flushPerIterations = Helpers.property("flush_per_iterations").toInt
    val List(startN, endN) = List("start_n", "end_n").map(i => Helpers.property(i).toInt)

    //val testCases = generateAllCases(startN, endN)
    val testCases = pointsAroundTurning(startN, endN)
    //perInstanceCases(startN, endN)  //pointsAroundTurning(startN, endN)  //generateTestCases()

    val endValue = testCases.last
    println("> " + testCases.length + " test cases to process: [" + testCases.head + "..." + endValue + "]...")

    timeWriter.println(""""start value:",""" + testCases.head + ""","end value:",""" + endValue)
    timeWriter.println(""""aggregated number of values","aggregated seconds"""")

    // Generate keys and compile Fairplay script
    if (Helpers.property("to_generate_keys").equals("true")) {
      Mediator.generateKeys()
      Mediator.compile()
    }

    // start computing ln(x) securely
    for (xValue <- testCases) {
      println("> x = " + xValue)
      count += 1

      Helpers.prepareInputs(BigInteger.valueOf(xValue))

      // Run Bob and Alice
      val bobArgs = Array[String]()
      //if (count == 1) bobArgs :+= "init"  //compile and generate keys only once

      AutomatedTest.main(bobArgs)

      val (_, bobOutputs) = readOutputs()

      // securely compute ln(x) and decrypt final result
      val computedResult = Mediator.actualLn(bobOutputs(0), bobOutputs(1), 10).doubleValue()

      // compute plain result
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
  }

  /**
   * Input x, compute ln(x) encryption result
   * @param xValue x as in ln(x)
   * @param toInit whether to generate keys/compile Fairplay script or not
   * @return encryption of ln(x) result
   */
  def lnWrapper(xValue: BigInteger, toInit: Boolean = false, writer: PrintWriter = null): BigInteger = {
    Helpers.prepareInputs(xValue)

    // Run Bob and Alice
    var bobArgs = Array[String]()
    if (toInit) bobArgs :+= "init"  //compile and generate keys only once

    val startedAt = System.currentTimeMillis()

    AutomatedTest.main(bobArgs)
    val fairplayTime = System.currentTimeMillis()

    val (_, bobOutputs) = readOutputs()

    val result = Mediator.secureLn(bobOutputs(0), bobOutputs(1))

    if (writer != null) writer.print( (fairplayTime - startedAt) + "," + (System.currentTimeMillis() - fairplayTime) )

    result
  }

  /**
   * Given numerator, denominator, compute actual division result
   * @param numerator numerator in plain value
   * @param denominator denominator in plain value
   * @param toInit whether or not to generate keys/compile Fairplay
   */
  //TODO remove cheat about determining result sign
  def runDivision( numerator: BigInteger, denominator: BigInteger, coefficient: Int = 1, timerWriter: PrintWriter = null,
                   toInit: Boolean = false, someone: Paillier = new Paillier(Helpers.getPublicKey()) ): Double = {
    val paillierNSquared = Helpers.getPublicKey().getN.pow(2)

    if (timerWriter != null) timerWriter.print(denominator + ",")
    val numeratorLn = lnWrapper(numerator.abs, toInit, timerWriter)  //DEBUG no init
    // to delimiter time records
    if (timerWriter != null) timerWriter.print(",")
    val denominatorLn = lnWrapper(denominator, false, timerWriter)
    if (timerWriter != null) timerWriter.println

    val fieldN = KeyGen.PaillierThresholdKeyLoad(new File(Helpers.property("data_directory"), Helpers.property("private_keys")).toString)(0).getN
    val diff = someone.add( if (coefficient > 1) someone.multiply(numeratorLn, coefficient).mod(paillierNSquared) else numeratorLn,
                             someone.multiply(denominatorLn, -1).mod(paillierNSquared) ).mod(paillierNSquared)

    val negative = (numerator.pow(2).divide(denominator).compareTo(BigInteger.ONE) < 0)

    //DEBUG only (change to true for debug purpose)
    if (false) {
      println("----Begin DEBUG: ")
      println("  NUM length: " + numerator.toString.length)
      println("  DEN length: " + denominator.toString.length)
      println("----")
      println("> Field size = " + fieldN.toString.length)
      println("  ln(num) = " + Mediator.decryptLn(numeratorLn))
      println("  ln(den) = " + Mediator.decryptLn(denominatorLn))
      println("  ln diff = " + Mediator.decryptLn(diff, 10, negative))
    }


    math.exp(Mediator.decryptLn(diff, 10, negative))
  }

  /**
   * Inverse-variance based meta-analysis
   * @param inputFile  file containing encrypted inputs (provided by Data Owners).
   *                   Note: each input file may consist of multiple experiments, each of which spans across several rows
   * @param resultFile  file to save final results to
   */
  def inverseVarianceExperiment(inputFile: String = Helpers.property("encrypted_data_file"),
                                resultFile: String = Helpers.property("final_result_file")) = {
    // to track division time breakdowns
    val divisionWriter = new java.io.PrintWriter(new java.io.File("data/division_time_breakdown.csv"))
    divisionWriter.println(""""denoninator (primary key)","Fairplay (numerator)","oblivious polynomial evaluation (numerator)","Fairplay (denominator)","oblivious polynomial evaluation (denominator)"""")
    // to track final result
    val writer = new java.io.PrintWriter(new java.io.File(resultFile))

    // denominator (sum(w_i)), numerator (sum(beta * w_i))

    //val flushPerIterations = Helpers.property("flush_per_iterations").toInt * 3
    writer.println("""quotient(secure),quotient(plain),"absolute error","relative error"""" +
      ""","decrypted numerator","decrypted denominator","SMC time (seconds)","division time (seconds)"""")

    // to track whether a new experiment starts
    // A new experiment is one that has different combination of control conditions
    var experimentFlag = ""
    var oneExperiment = new Array[Array[String]](0)
    val validLines = io.Source.fromFile(inputFile).getLines().toArray.drop(1)
    val lastIndex = validLines.size - 1

    for ( (line, indx) <- validLines.zipWithIndex; record = line.split(",")) {
      // We combine all control attribute names to get the "primary key" for current experiment
      val tmpFlag = record.slice(4, 20).mkString("")
      if (experimentFlag.equals("")) experimentFlag = tmpFlag

      // When encountered first row of *next* experiment, process previous experiment and reset variables
      // NOTE: we need to handle last row separately (otherwise the last experiment will get ignored)
      if ( (! tmpFlag.equals(experimentFlag)) || lastIndex == indx ) {
        // First of all, process *previous* experiment
        if (lastIndex == indx) oneExperiment :+= record.slice(0, 4)
        // Refer to the declaration of following method for return result details
        val results = Mediator.inverseVariance(oneExperiment, divisionWriter)
        writer.println(results._1 + "," + results._4 + "," + (results._1 - results._4) + ","
          + (results._1 - results._4)/results._4 + "," + results._5 + "," + results._6 + "," + results._2/1000.0 + "," + results._3/1000.0)

        // flush buffer after certain number of experiments
        //if (indx % flushPerIterations == 0) {
          println("> Current data row index: " + indx + "\n  Writing to result file...")
          writer.flush()
          divisionWriter.flush()
        //}

        // reset for next experiment
        oneExperiment = new Array[Array[String]](0)
        experimentFlag = tmpFlag
      }

      oneExperiment :+= record.slice(0, 4)
    }

    divisionWriter.close()
    writer.close()
  }


  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    // document current experiment
    createDataDir()
    copyFiles()
    generateReadme()

    runLn(startedAt)

    //runDivision(new BigInteger("4000000"), new BigInteger("4"), toInit = false)

    //Mediator.inverseVariance()
    //inverseVarianceExperiment()


    println("\nExperiment process finished in " + (System.currentTimeMillis() - startedAt) / 1000 + " seconds.")
  }
}