package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 */

import java.io._
import java.math.BigInteger
import java.util
import paillierp.Paillier
import paillierp.key.KeyGen

import paillierp.PaillierThreshold
import java.math.BigDecimal

import org.apache.commons.math3.util.ArithmeticUtils

import SFE.BOAL.{Bob, MyUtil}
import java.net.ServerSocket


object Mediator {
  val K_TAYLOR_PLACES = Helpers.property("k_taylor_places").toInt  //it seems 7 is the cap. Larger number causes out-of-range crash
  val LCM = (2 to K_TAYLOR_PLACES).foldLeft(1)((a, x) => ArithmeticUtils.lcm(a, x))
  val MaxN = Helpers.property("max_exponent_n").toInt
  val POWER_OF_TWO = math.pow(2, MaxN)
  //2^N
  //Currently Paillier max field bit size is set to 2048. A size > 1024 would be really slow
  //512
  val FieldBitsMax = ((MaxN + 2) * K_TAYLOR_PLACES +
    (math.log(MaxN) / math.log(2) + math.log(Helpers.getMultiplier()  * 100) / math.log(2)).ceil.toInt)
  //val FieldMax = new BigInteger("%.0f".format(math.pow(2, FieldBitsMax)))

  val FairplayFile = Helpers.property("fairplay_script")

  /**
   * Generate and store Paillier Threshold keys to file
   * @param length  field length as in Paillier encryption. Refer to KeyGen.PaillierThresholdKey
   * @param seed  random seed in Paillier encryption
   * @return  file path to private and public keys
   */
  def generateKeys(length: Int = FieldBitsMax, seed: Long = new util.Random().nextLong()) = {
    println("Key bit length: " + length)
    val dataDir = Helpers.property("data_directory")
    val privateKeyFile = new File(dataDir, Helpers.property("private_keys")).toString
    val publicKeyFile = new File(dataDir, Helpers.property("public_keys")).toString
    val totalParties = Helpers.property("total_parties").toInt
    val thresholdParties = Helpers.property("threshold_parties").toInt

    // Generate and store private keys
    KeyGen.PaillierThresholdKey(privateKeyFile, length, totalParties, thresholdParties, seed)

    // Store public key
    val publicKey = KeyGen.PaillierThresholdKeyLoad(privateKeyFile)(0).getPublicKey()
    val buffer = new java.io.BufferedOutputStream(new java.io.FileOutputStream(publicKeyFile))
    val output = new java.io.ObjectOutputStream(buffer)
    output.writeObject(publicKey)
    output.close()
    buffer.close()

    (privateKeyFile, publicKeyFile)
  }

  /**
   * Perform secure meta-analysis on given input set (one experiment)
   * @param records input data from different contributing sites
   * @param divisionWriter file writer to track division time breakdown
   * @return tuple of (computed division, SMC time, division time, plain division, decrypted numberator, decrypted denominator)
   */
  def inverseVariance(records: Array[Array[String]], divisionWriter: PrintWriter) = {
    val paillierNS = Helpers.paillierNS()
    val someone = new Paillier(Helpers.getPublicKey())
    var weightSum, betaWeightSum = someone.encrypt(BigInteger.ZERO).mod(paillierNS)
    // sum(weight_i) of denominator; sum(beta_i * weight_i) of numerator respectively
    //DEBUG for verification only
    var testWeightSum, testBetaWeightSum = 0.0

    val startedAt = System.currentTimeMillis()
    for (record <- records) {
      weightSum = someone.add(weightSum, new BigInteger(record(0))).mod(paillierNS)
      betaWeightSum = someone.add(betaWeightSum, new BigInteger(record(1))).mod(paillierNS)

      //DEBUG for verification
      testWeightSum += record(2).toDouble
      testBetaWeightSum += record(3).toDouble
    }
    val smcTime = System.currentTimeMillis()

    //-- Secure Division
    //val decryptedNumerator = decryptData(betaWeightSum)
    //val decryptedDenominator = decryptData(weightSum)
    var computedDivision = math.sqrt(
      Experiment.runDivision(betaWeightSum, weightSum, 2, divisionWriter) / Helpers.getMultiplier())
    val plainDivision = testBetaWeightSum / math.sqrt(testWeightSum)
    // to determine the sign of final result
    if (plainDivision < 0) computedDivision = - computedDivision

    (computedDivision, smcTime - startedAt, System.currentTimeMillis() - smcTime,
      plainDivision, "NA", "NA")
  }


  /**
   * Obtain coefficients for binomial expansion
   * @param constA  constant alpha1 as in binomial polynomial
   * @param powerI  the power size of the expansion
   * @return  vector with all coefficients
   */
  def polynomialCoefficients(constA: BigInteger, powerI: Int) = {
    val coefficients = Array.fill[BigInteger](K_TAYLOR_PLACES + 1)(BigInteger.ZERO)

    //TODO reduce unnecessary power computation
    //val tmp = new BigInteger("%.0f" format math.pow(POWER_OF_TWO, K_TAYLOR_PLACES - powerI) * math.pow(-1, powerI - 1))
    var tmp = new BigInteger("%.0f" format POWER_OF_TWO).pow(K_TAYLOR_PLACES - powerI).multiply(BigInteger.valueOf(-1).pow(powerI - 1))
    tmp = tmp.multiply(BigInteger.valueOf(LCM/powerI))

    for (j <- 0 to powerI) {
      coefficients(j) = constA.pow(powerI - j).multiply(BigInteger.valueOf(ArithmeticUtils.binomialCoefficient(powerI, j))).multiply(tmp)
    }

    coefficients
  }


  /*
  def dotProduct(coefficients: Array[BigInteger], encryptedPowers: Array[BigInteger]) = {
    //TODO reduncancy
    val privateKeys = Manager.prepareData(toVerifyEncryption = false)
    val publicKey = privateKeys(0).getPublicKey
    val someone = new Paillier(publicKey)

    (for ((a, b) <- coefficients zip encryptedPowers) yield someone.multiply(b, a)).foldLeft(someone.encrypt(BigInteger.ZERO))((m, x) => someone.add(m, x))
  }
  */


  /**
   * Compile Fairplay script
   * NOTE: it seems only ONE compilation is needed
   * TODO read from config script file name
   */
  def compile() = {
    SFE.BOAL.Bob.main(Array("-c", FairplayFile))
  }


  /**
   * Run Bob (starts socket server)
   * Note: socket server blocks the thread
   * TODO read filename from config
   */
  def runBob() = {
  }

  /**
   * Decrypt ciphertext (without further processing about expected negative results)
   * @param encrypted ciphertext
   * @return always-positive plain value result
   */
  def decryptDataNoProcessing(encrypted: BigInteger) = {
    val configs = Helpers.properties("data_directory", "private_keys", "threshold_parties")
    val privateKeys = KeyGen.PaillierThresholdKeyLoad(new File(configs(0), configs(1)).toString)
    val parties = for (k <- privateKeys.take(configs(2).toInt)) yield new PaillierThreshold(k)
    //val parties = privateKeys.take(configs(2).toInt).map(new PaillierThreshold(_))
    parties(0).combineShares((for (p <- parties) yield p.decrypt(encrypted)): _*).mod(privateKeys(0).getN)
  }

  /**
   * Decrypt ciphertext (support for negative results)
   * @param encrypted ciphertext
   * @return plain value result
   */
  def decryptData(encrypted: BigInteger) = {
    val decrypted = decryptDataNoProcessing(encrypted)
    val paillierN = Helpers.getPublicKey().getN
    if (decrypted.bitLength < paillierN.bitLength - 1) decrypted else decrypted.subtract(paillierN)
  }

  /**
   * Phase 2 of secure ln(x): Taylor expansion
   * @param constA  alpha1 as in binomial expansion
   * @return  encrypted result of Taylor expansion
   */
  def taylorExpansion(constA: BigInteger, encryptedPowers: Array[BigInteger]) = {
    // Prepare constant coefficients
    var coefficients = polynomialCoefficients(constA, 1)
    val paillierNSquared = Helpers.getPublicKey().getN.pow(2)

    // combine coefficients of the same power sizes
    for (variableI <- 2 to K_TAYLOR_PLACES) {
      val nextVector = polynomialCoefficients(constA, variableI)
      coefficients = for ((a, b) <- coefficients zip nextVector) yield a.add(b)
    }

    // Perform Taylor expansion (assemble coefficients and variables)
    //TODO read Alice's input via network
    //val encryptedPowers = MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Alice.power")
    val someone = new Paillier(Helpers.getPublicKey())

    (encryptedPowers zip coefficients).foldLeft(someone.encrypt(BigInteger.ZERO)) { (a, x) =>
      someone.add(a, someone.multiply(x._1, x._2).mod(paillierNSquared)).mod(paillierNSquared)
    }


    /* The verbose version of above
    //DEBUG only
    val paillierN = someone.getPublicKey.getN
    println("Paillier N: " + paillierN.bitCount())
    var tmpResult = someone.encrypt(BigInteger.ZERO)  //.mod(someone.getPublicKey.getN)
    val nSquared = someone.getPublicKey.getN.multiply(someone.getPublicKey.getN)
    for (i <- 0 to encryptedPowers.size - 1) {
      println("Before: " + tmpResult.compareTo(someone.getPublicKey.getN))
      val tmpMultiple = someone.multiply(encryptedPowers(i), coefficients(i))
      tmpResult = someone.add(tmpResult, tmpMultiple)
      println("after: " + tmpResult.compareTo(someone.getPublicKey.getN))
    }

    tmpResult
    */
  }


  /**
   * Compute ln(x) securely (the second phase: Taylor expansion)
   * @param alpha  Bob's input alpha
   * @param beta  Bob's input beta
   * @return  encryption of scaled-up ln(x)
   */
  def secureLn(alpha: BigInteger, beta: BigInteger,
               alicePowers: Array[BigInteger], aliceBeta: BigInteger) = {
    val someone = new Paillier(Helpers.getPublicKey())
    val taylorResult = taylorExpansion(alpha, alicePowers)
    val paillierNS = Helpers.paillierNS()

    //TODO transfer from socket
    val tmp = someone.add(beta, aliceBeta).mod(paillierNS)
    someone.add(taylorResult, tmp).mod(paillierNS)
  }

  /**
   * Decrypt enc(ln(x))
   * @param encryptedLn ln(x) encryption
   * @param scale
   * @return ln(x) decryption
   */
  def decryptLn(encryptedLn: BigInteger, scale: Int = 10): Double = {
    val divisor = new BigInteger("%.0f" format Mediator.POWER_OF_TWO).pow(Mediator.K_TAYLOR_PLACES).multiply(BigInteger.valueOf(Mediator.LCM))
    new BigDecimal(decryptData(encryptedLn)).divide(new BigDecimal(divisor), scale, BigDecimal.ROUND_HALF_UP).doubleValue()
  }

/*
  //TODO for testing only
  def divide(numerator: BigInteger, denominator: BigInteger) = {
    val someone = new Paillier(getPublicKey())
    val diff = someone.add(numerator, someone.multiply(denominator, -1))

    // Get ln(x) first
    val bobOutput = MyUtil.readResult(FairplayFile + ".Bob.output").filter(_ != null)
    secureLn(bobOutput(0), bobOutput(1))

    Experiment.prepareInputs(BigInteger.valueOf(xValue))

    // Run Bob and Alice
    var bobArgs = Array[String]()
    bobArgs :+= "init"  //compile and generate keys only once

    AutomatedTest.main(bobArgs)

    val (_, bobOutputs) = readOutputs()

    val computedResult = Mediator.actualLn(bobOutputs(0), bobOutputs(1), 10).doubleValue()
  }
  */

  /**
   * Receive intermediate result from Manager/Alice
   * @return
   */
  def receiveData(socketPort: Int): Tuple2[Array[BigInteger], BigInteger] = {
    var encryptedPowers = Array[BigInteger]()
    var beta = BigInteger.ZERO

    println("Waiting for return result...")
    val listener = new ServerSocket(socketPort)

    try {
      val socket = listener.accept()

      try {
        val input = new ObjectInputStream(socket.getInputStream())
        encryptedPowers = input.readObject().asInstanceOf[Array[BigInteger]]
        beta = input.readObject().asInstanceOf[BigInteger]

        // DEBUG only
        println("[from Alice] beta: " + beta)
        println("[From Alice] beta powers: " + encryptedPowers.mkString("  "))

      } catch { case e: Exception =>
        e.printStackTrace()
      } finally {
        socket.close()
      }

    } catch { case ex: Exception =>
      ex.printStackTrace()
    } finally {
      listener.close()
    }

    (encryptedPowers, beta)
  }

  /**
   * Input x, compute ln(x) encryption result
   * @param xValue x as in ln(x)
   * @param toInit whether to generate keys/compile Fairplay script or not
   * @return encryption of ln(x) result
   */
  def lnWrapper(xValue: BigInteger, toInit: Boolean = false, writer: PrintWriter = null, socketPortOffset: Int = 0): BigInteger = {
    val inputs = Helpers.prepareInputs(xValue)

    // Run Bob and Alice
    var inputArgs = Array[String]()
    //if (toInit) inputArgs :+= "init"  //compile and generate keys only once
    inputArgs ++= inputs
    inputArgs :+= socketPortOffset.toString

    val startedAt = System.currentTimeMillis()

    // use garbled circuit to perform first phase of ln(x)
    new BigInteger(AutomatedTest.main(inputArgs))

    // track runtime
    /*
    if (writer != null)
      writer.print( (fairplayTime - startedAt) + "," + (System.currentTimeMillis() - fairplayTime) )
    */
  }

  /**
   * Run Mediator's role (including Bob's)
   * @param args [only-init]: generate keys and compile Fairplay script only (no further running)
   *             [with-init]: before running Bob's role, will also do initialization
   */
  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    //println("> Plain input: " + Helpers.getPlainInput())

    //inverseVariance(Helpers.property("encrypted_data_file"), Helpers.property("final_result_file"), true)

    //--- Run Fairplay ---
    // Generate keys only when forced to or no keys exist
    if ( args.length > 0 && args(0).equals("init") || (! new File(Helpers.property("data_directory"), Helpers.property("private_keys")).exists()) ) {
      //generateKeys()
      compile()
    }


    //-- Run Bob's role ---
    var inputArgs = Array("-r", Helpers.property("fairplay_script"), "dj2j", "4")
    if (args.length > 1) {
      // customize socket port
      inputArgs :+= (Helpers.property("socket_port").toLong + (if (args.length > 2) args(2).toInt else 0)).toString
      inputArgs :+= args(0)
    }

    val bobOutputs = Bob.main(inputArgs).filter(_ != null)


    //val Array(alpha, beta) = Helpers.getFairplayResult("Bob")
    // store my beta shares
    //Helpers.storeBeta("Bob", beta)

    val aliceOutputs = receiveData(inputArgs(4).toInt + 1)

    // -- END OF garbled circuit --


    val fairplayTime = System.currentTimeMillis()


    val resultEncryption = Mediator.secureLn(bobOutputs(0), Helpers.encryptBeta(bobOutputs(1)), aliceOutputs._1, aliceOutputs._2)


    //--- Compute ln(x) alone ---
    //println("Computed: " + actualLn(alpha, beta, 10))

    //getPublicKey()
    //inverseVariance()
    //distributeKeys()


    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000.0 + " seconds.")

    println(resultEncryption)
  }
}