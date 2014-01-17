package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Run whole protocol on test data
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 */

import java.io.{FileInputStream, FileOutputStream, PrintWriter, File}
import SFE.BOAL.MyUtil
import java.math.BigInteger
import paillierp.Paillier
import concurrent.{future, blocking, Future, Await}
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import java.util.concurrent.{Callable, FutureTask, Executors}
import Program.{EstimateNClient, EstimateNServer}
import scala.Array
import fastgc.CircuitQuery
import scala.util.{Failure, Success}

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
    ( Helpers.readFairplayResult(PathPrefix + "Alice.output").filter(_ != null).asInstanceOf[Array[BigInteger]],
      Helpers.readFairplayResult(PathPrefix + "Bob.output").filter(_ != null).asInstanceOf[Array[BigInteger]] )
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
      //Mediator.compile()
    }

    // start computing ln(x) securely
    for (xValue <- testCases) {
      println("> x = " + xValue)
      count += 1

      /*
      Helpers.prepareInputs(BigInteger.valueOf(xValue))

      // Run Bob and Alice
      val bobArgs = Array[String]()
      //if (count == 1) bobArgs :+= "init"  //compile and generate keys only once

      AutomatedTest.main(bobArgs)

      val (_, bobOutputs) = readOutputs()

      */

      // securely compute ln(x) and decrypt final result
      //val computedResult = Mediator.actualLn(bobOutputs(0), bobOutputs(1), 10).doubleValue()
      val computedResult = Mediator.decryptLn(Mediator.lnWrapper(BigInteger.valueOf(xValue)))

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
   * Compute actual division, given numerator and denominator
   * @param numeratorEncryption numerator encryption
   * @param denominatorEncryption denominator encryption
   * @param toInit whether or not to generate keys/compile Fairplay
   */
  def runDivision( numeratorEncryption: BigInteger, denominatorEncryption: BigInteger, coefficient: Int = 1,
                   timerWriter: PrintWriter = null, toInit: Boolean = false,
                   someone: Paillier = new Paillier(Helpers.getPublicKey()) ): Double = {
    val paillierNSquared = Helpers.getPublicKey().getNSPlusOne
    val decryptionFuture: List[Future[BigInteger]] = List(numeratorEncryption, denominatorEncryption).map(a =>
      future { blocking {Mediator.decryptData(a)} })
    val decryptions = Await.result(Future.sequence(decryptionFuture), 20 second)

    //println("LnWrapper: " + lnWrapper(BigInteger.valueOf(100), toInit = false, writer = null,
    //              bobPort = 3490, alicePort = 3491, socketPort = 3496)  )

    val pool = Executors.newFixedThreadPool(4)
    val numeratorFuture = new FutureTask[BigInteger]( new Callable[BigInteger] {
      def call(): BigInteger = Mediator.lnWrapper(decryptions(0).abs, toInit = false,
        writer = timerWriter, bobPort = 3490, alicePort = 3491, socketPort = 3496)
    })
    val denominatorFuture = new FutureTask[BigInteger]( new Callable[BigInteger] {
      def call(): BigInteger = Mediator.lnWrapper(decryptions(1), toInit = false,
        writer = timerWriter, bobPort = 3492, alicePort = 3493, socketPort = 3497)
    })

    pool.execute(numeratorFuture)
    pool.execute(denominatorFuture)

    val numeratorLn = numeratorFuture.get()
    val denominatorLn = denominatorFuture.get()

    //val numeratorLn = Mediator.lnWrapper(decryptions(0).abs, toInit, timerWriter)
    // to delimiter time records
    //if (timerWriter != null) timerWriter.print(",")

    //val denominatorLn = Mediator.lnWrapper(decryptions(1), false, timerWriter)
    //if (timerWriter != null) timerWriter.println

    val diff = someone.add( if (coefficient > 1) someone.multiply(numeratorLn, coefficient).mod(paillierNSquared) else numeratorLn,
                             someone.multiply(denominatorLn, -1).mod(paillierNSquared) ).mod(paillierNSquared)

    math.exp(Mediator.decryptLn(diff, 10))
  }


  /**
   * Vary the number of participating sites to test scalability
   * @return
   */
  def variableSites() = {
    val variableSitesFile = new File(Helpers.property("encrypted_data_with_variable_sites"))
    val writer = new java.io.PrintWriter(variableSitesFile)

    val validLines = io.Source.fromFile(Helpers.property("encrypted_data_file")).getLines().toArray
    writer.println(validLines(0))

    var experimentFlag = ""
    var oneExperiment = new Array[String](0)
    val lastIndex = validLines.size - 2

    println("> To support variable sites...")
    for ( (line, indx) <- validLines.drop(1).zipWithIndex; record = line.split(",")) {
      // We combine all control attribute names to get the "primary key" for current experiment
      val tmpFlag = record.slice(4, 20).mkString("::")
      if (experimentFlag.isEmpty) experimentFlag = tmpFlag

      // When encountered first row of *next* experiment, process previous experiment and reset variables
      // NOTE: we need to handle last row separately (otherwise the last experiment will get ignored)
      if ( (! tmpFlag.equals(experimentFlag)) || lastIndex == indx ) {
        // First of all, process *previous* experiment
        if (lastIndex == indx) oneExperiment :+= line
        // process current experiment
        for (sitesCount <- 1 to oneExperiment.size) {
          oneExperiment.take(sitesCount).foreach(perSiteLine => writer.println(perSiteLine + "," + sitesCount))
        }

        println("  Current data row index: " + indx)

        // reset for next experiment
        oneExperiment = new Array[String](0)
        experimentFlag = tmpFlag
      }

      oneExperiment :+= line
    }

    try {
      writer.close()
      variableSitesFile.renameTo(new File(Helpers.property("encrypted_data_file")))
    } catch {
      case e: Exception => println("ERROR in renaming variable_sites file.")
    }
  }

  /**
   * Experiment with inverse-variance based meta-analysis
   * Partition into multiple experiments according to experiment identifiers
   * Note: for division: denominator (sum(w_i)), numerator (sum(beta * w_i))
   * @param inputFile  file containing encrypted inputs (provided by Data Owners).
   *                   Note: each input file may consist of multiple experiments, each of which spans across several rows
   * @param resultFile  file to save final results to
   */
  def inverseVarianceExperiment(inputFile: String = Helpers.property("encrypted_data_file"),
                                resultFile: String = Helpers.property("final_result_file")) = {
    val writer = new java.io.PrintWriter(new java.io.File(resultFile))
    val divisionWriter = new java.io.PrintWriter(new java.io.File("data/division_time_breakdown.csv"))

    divisionWriter.println("denoninator_primary_key,Fairplay_numerator,oblivious_polynomial_evaluation_numerator" +
      ",Fairplay_denominator,oblivious_polynomial_evaluation_denominator")
    // to save final result
    val writer = new java.io.PrintWriter(new java.io.File(resultFile))
    writer.println("quotient_secure,quotient_plain,absolute_error,relative_error" +
      ",decrypted_numerator,decrypted_denominator,SMC_time_seconds,division_time_seconds,experiment_identifier")

    // to mark whether a new experiment starts (compare set of identifiers)
    var experimentFlag = ""
    var oneExperiment = new Array[Array[String]](0)
    val validLines = io.Source.fromFile(inputFile).getLines().toArray.drop(1)
    val lastIndex = validLines.size - 1


    /*

    for ((line, indx) <- validLines.zipWithIndex; record = line.split(",")) {
      // experiment identifiers start from 5th column
      val tmpFlag = record.slice(4, 20).mkString("::").toUpperCase
      if (experimentFlag.equals("")) experimentFlag = tmpFlag

      // encountered with "next" experiment or last row of whole dataset
      if ( (! tmpFlag.equals(experimentFlag)) || lastIndex == indx ) {
        if (lastIndex == indx) oneExperiment :+= record.slice(0, 4)

        // process "previous" experiment first
        val results = Mediator.inverseVariance(oneExperiment, divisionWriter)
        writer.println(results._1 + "," + results._4 + "," + (results._1 - results._4) + ","
          + "," + results._5 + "," + results._6 + "," +
          results._2/1000.0 + "," + results._3/1000.0 + "," + experimentFlag)


        println("> Processed till row # %d / %d ; Saving result...".format(indx, lastIndex))
        writer.flush()
        divisionWriter.flush()

        // reset for next experiment
        oneExperiment = new Array[Array[String]](0)
        experimentFlag = tmpFlag
      }

      oneExperiment :+= record.slice(0, 4)
    }

    try {
      divisionWriter.close()
      writer.close()
    } catch {
      case e: Exception => println("ERROR: result files not properly closed")
    }


    println("Before server future")
    val circuitServer = new EstimateNServer(80, 80)
    circuitServer.setInputs(new BigInteger("1"))
    val serverFuture = future { circuitServer.runOffline() }

    println("Before client future")
    val circuitClient = new EstimateNClient(80)
    circuitClient.setInputs(new BigInteger("5"))
    val clientFuture = future { circuitClient.runOffline() }

    println("Starts to await..")
    //Await.result(serverFuture, 40 seconds)
    println("Starts to await client..")
    Await.result(clientFuture, 40 seconds)

    val bobClient = new GCClient()
    val bobFuture = future { bobClient.run(3491, Array("haha", "hello")) }
    // Alice
    val aliceClient = new GCClient()
    val aliceFuture = future { aliceClient.run(3492, Array("client", "helloclient")) }

    Await.result(bobFuture, 30 second)

    Await.result(aliceFuture, 30 second)

*/
//
//    val bobClient = new CircuitQuery()
//    val bobFuture = future { bobClient.run(3491, Array("1", "2", "3")) }
//    // Alice
//    val aliceClient = new CircuitQuery()
//    val aliceFuture = future { aliceClient.run(3492, Array("5", "2", "3") ) }

//    bobFuture.onComplete {
//      case Success(results) => println(results.toString)
//      case Failure(t) => println("Error! " + t.getStackTrace)
//    }
//
//    aliceFuture.onComplete {
//      case Success(results) => println(results.toString)
//      case Failure(t) => println("Error! " + t.getStackTrace)
////    }
//
//    Await.result(bobFuture, 30 second)
//    Await.result(aliceFuture, 30 second)

    val bobClient = new CircuitQuery()
    val bobFuture = new Thread(new Runnable {
      def run() {
        bobClient.run(3491, Array("1"))
      }
    } )

    val aliceClient = new CircuitQuery()
    val aliceFuture = new Thread(new Runnable {
      def run() {
        aliceClient.run(3492, Array("5"))
      }
    } )

    bobFuture.start()
    aliceFuture.start()

    println("DONE")
  }

  /**
   * Refer to below for param switches
   * @param args
   */
  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()

    createDataDir()

    // Compile Fairplay script as necessary
    if ( args.length > 0 && args(0).equals("init") ) {
      println("> To generate keys (takes time)...")
      Mediator.generateKeys()

      println("> To compile Fairplay script...")
      Mediator.compile()

    } else if ( args.length > 0 && args(0).equals("compile") ) {
      println("> To compile Fairplay script...")
      Mediator.compile()
      println("= END of compiling Fairplay script.")

    } else if (args.length > 0 && args(0).equals("ln")) {
      println("> Secure ln(x)...")
      // document current experiment
      copyFiles()
      generateReadme()

      runLn(startedAt)
      println("= END of secure ln(x)")

    } else {
      // Experiment on secure meta-analysis
      if (args.length > 0 && args(0).equals("variable-sites"))
        variableSites()

      println("> Secure meta-analysis...")
      inverseVarianceExperiment( inputFile = Helpers.property("encrypted_data_file"),
                                 resultFile = Helpers.property("final_result_file") )
      println("> END of secure meta-analysis.")
    }


    //runDivision(new BigInteger("4000000"), new BigInteger("4"), toInit = false)

    println("\nExperiment process finished in " + (System.currentTimeMillis() - startedAt) / 1000 + " seconds.")
  }
}