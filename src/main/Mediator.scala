package main

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 */

import java.io._
import java.math.BigInteger
import java.net.{Socket, ServerSocket}
import java.util
import java.util.Random
import org.apache.commons.math3.linear.{RealVector, ArrayRealVector}
import paillierp.Paillier
import paillierp.key.KeyGen
import paillierp.key.PaillierPrivateThresholdKey
import paillierp.key.PaillierKey

import paillierp.PaillierThreshold
import java.math.{BigDecimal, RoundingMode}

import org.apache.commons.math3.util.ArithmeticUtils

import SFE.BOAL.{Bob, MyUtil}

import java.net.UnknownHostException
import test.AutomatedTest

object Mediator {
  val K_TAYLOR_PLACES = Helpers.property("k_taylor_places").toInt  //it seems 7 is the cap. Larger number causes out-of-range crash
  val LCM = (2 to K_TAYLOR_PLACES).foldLeft(1)((a, x) => ArithmeticUtils.lcm(a, x))
  val MaxN = Helpers.property("max_exponent_n").toInt
  val POWER_OF_TWO = math.pow(2, MaxN)
  //2^N
  //Currently Paillier max field bit size is set to 2048. A size > 1024 would be really slow
  //512
  val FieldBitsMax = ((MaxN + 2) * K_TAYLOR_PLACES +
    (math.log(MaxN) / math.log(2) + math.log(Helpers.MULTIPLIER  * 100) / math.log(2)).ceil.toInt)
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

  // Return: (computed division, SMC time, division time, plain division, decrypted numberator, decrypted denominator)
  def inverseVariance(records: Array[Array[String]], divisionWriter: PrintWriter) = {
    val startedAt = System.currentTimeMillis()

    val paillierNS = Helpers.paillierNS()
    val someone = new Paillier(Helpers.getPublicKey())
    // sum(weight_i) of denominator; sum(beta_i * weight_i) of numerator respectively
    var weightSum, betaWeightSum = someone.encrypt(BigInteger.ZERO).mod(paillierNS)
    //DEBUG for verification only
    var testWeightSum, testBetaWeightSum = 0.0

    for (record <- records) {
      weightSum = someone.add(weightSum, new BigInteger(record(0))).mod(paillierNS)
      betaWeightSum = someone.add(betaWeightSum, new BigInteger(record(1))).mod(paillierNS)

      //DEBUG for verification
      testWeightSum += record(2).toDouble
      testBetaWeightSum += record(3).toDouble
    }
    val smcTime = System.currentTimeMillis()

    //-- Secure Division
    val decryptedNumerator = decryptData(betaWeightSum, (testBetaWeightSum < 0))
    val decryptedDenominator = decryptData(weightSum, (testWeightSum < 0))
    var computedDivision = math.sqrt(Experiment.runDivision(decryptedNumerator, decryptedDenominator, 2, divisionWriter) / Helpers.MULTIPLIER)
    val plainDivision = testBetaWeightSum / math.sqrt(testWeightSum)
    // to determine the sign of final result
    if (plainDivision < 0) computedDivision = - computedDivision

    // note the order of return results
    (computedDivision, smcTime - startedAt, System.currentTimeMillis() - smcTime,
      plainDivision, decryptedNumerator, decryptedDenominator)
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
   * Decrypt ciphertext
   * @param encrypted ciphertext
   * @param negative whether the final result is going to be negative
   * @return plain value result
   * TODO remove the "negative" signal parameter
   */
  def decryptData(encrypted: BigInteger, negative: Boolean = false) = {
    val privateKeys = KeyGen.PaillierThresholdKeyLoad(new File(Helpers.property("data_directory"), Helpers.property("private_keys")).toString)
    val parties = for (k <- privateKeys.take(Helpers.property("threshold_parties").toInt)) yield new PaillierThreshold(k)
    val decrypted = parties(0).combineShares((for (p <- parties) yield p.decrypt(encrypted)): _*).mod(privateKeys(0).getN)

    if (negative) decrypted.subtract(privateKeys(0).getN) else decrypted
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
   * Compute ln(x) securely
   * @param alpha  Bob's input alpha
   * @param beta  Bob's input beta
   * @return  encryption of scaled-up ln(x)
   */
  def secureLn(alpha: BigInteger, beta: BigInteger) = {
    val alicePowers = MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Alice.power")

    val someone = new Paillier(Helpers.getPublicKey())
    val taylorResult = taylorExpansion(alpha, alicePowers)
    val paillierNS = Helpers.paillierNS()

    //TODO transfer from socket
    val betas = (for (i <- Array("Alice", "Bob")) yield MyUtil.readResult(MyUtil.pathFile(FairplayFile) + "." + i + ".beta")(0))
    val tmp = someone.add(betas(0), betas(1)).mod(paillierNS)
    someone.add(taylorResult, tmp).mod(paillierNS)
  }

  def decryptLn(encryptedLn: BigInteger, scale: Int = 10, negative: Boolean = false): Double = {
    var tmp = decryptData(encryptedLn)
    if (negative) {
      val fieldN = KeyGen.PaillierThresholdKeyLoad(new File(Helpers.property("data_directory"), Helpers.property("private_keys")).toString)(0).getN
      tmp = tmp.subtract(fieldN)
    }

    val divisor = new BigInteger("%.0f" format Mediator.POWER_OF_TWO).pow(Mediator.K_TAYLOR_PLACES).multiply(BigInteger.valueOf(Mediator.LCM))
    new BigDecimal(tmp).divide(new BigDecimal(divisor), scale, BigDecimal.ROUND_HALF_UP).doubleValue()
  }

  /**
   * Remember to modify multiplier in Fairplay script when customizing param *scale*
   * @alpha Bob's alpha
   * @beta Bob's beta
   * @return  decrypted value of ln(x)
   */
  def actualLn(alpha: BigInteger, beta: BigInteger, scale: Int = 6) = {
    val tmp = secureLn(alpha, beta)

    decryptLn(tmp)
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
   * Receive intermediate result from Manager
   * @return
   */
  def receiveData() = {
    // Read content
    //val fromOrigin = new ObjectInputStream(socket.getInputStream())
    //val (alicePowers, aliceBeta) = (fromOrigin.readObject().asInstanceOf[Array[BigInteger]], fromOrigin.readObject().asInstanceOf[BigInteger])

    // Receive data file
    //is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream))
    //val result = is.readObject().asInstanceOf[Array[BigInteger]]
    //MyUtil.saveResult(result, MyUtil.pathFile(FairplayFile) + ".Alice.power.transferred")
    //is.close()
    //socket.close()

    //val someone = new Paillier(getPublicKey())
    //val taylorResult = taylorExpansion(alpha, alicePowers)

    //socket.close()
    //fromOrigin.close()
  }

  // args = [firstRun?]
  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    println("> Plain input: " + Helpers.getPlainInput())

    //inverseVariance(Helpers.property("encrypted_data_file"), Helpers.property("final_result_file"), true)

    //--- Run Fairplay ---
    // Generate keys only when forced to or no keys exist
    if ( args.length > 0 && args(0).equals("init") || (! new File(Helpers.property("data_directory"), Helpers.property("private_keys")).exists()) ) {
      //generateKeys()
      compile()
    }

    // Will store output to file
    Bob.main(Array("-r",
                    Helpers.property("fairplay_script"), "dj2j",
                    "4",
                    Helpers.property("socket_port"))
    )


    val Array(alpha, beta) = Helpers.getFairplayResult("Bob")
    // store my beta shares
    Helpers.storeBeta("Bob", beta)

    //--- Compute ln(x) ---
    // First need to make sure Manager finishes running
    val tmpPowerFile = new File(MyUtil.pathFile(FairplayFile) + ".Alice.power")
    while (! tmpPowerFile.exists()) {
      Thread.sleep(100)
    }

    //println("Computed: " + actualLn(alpha, beta, 10))

    //getPublicKey()
    //inverseVariance()
    //distributeKeys()


    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000.0 + " seconds.")
  }
}