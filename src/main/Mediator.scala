package main

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 5/24/12
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
  val K_TAYLOR_PLACES = 5  //it seems 7 is the cap. Larger number causes out-of-range crash
  val LCM = (2 to K_TAYLOR_PLACES).foldLeft(1)((a, x) => ArithmeticUtils.lcm(a, x))
  val MaxN = 20
  val POWER_OF_TWO = math.pow(2, MaxN)
  //2^N
  //Currently Paillier max field bit size is set to 2048. A size > 1024 would be really slow
  //512
  val FieldBitsMax = 512 //((MaxN + 2) * K_TAYLOR_PLACES + (math.log(MaxN) / math.log(2) + math.log(Owner.MULTIPLIER  * 100) / math.log(2)).ceil.toInt)
  //val FieldMax = new BigInteger("%.0f".format(math.pow(2, FieldBitsMax)))

  val FairplayFile = Helpers.property("fairplay_script")

  /**
   * Generate and store Paillier Threshold keys to file
   * @param length  field length as in Paillier encryption. Refer to KeyGen.PaillierThresholdKey
   * @param seed  random seed in Paillier encryption
   * @return  file path to private and public keys
   */
  def generateKeys(length: Int = FieldBitsMax, seed: Long = new util.Random().nextLong()) = {
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
   * Aggregate study results from various sites
   * Inverse-variance (Effect-size) based approach for Meta-analysis
   * @param inputFile  file containing encrypted data
   */
  def inverseVariance(inputFile: String = Helpers.property("encrypted_data_file"),
                       resultFile: String = Helpers.property("final_result_file"),
                       toVerify: Boolean = false) = {
    val writer = new java.io.PrintWriter(new java.io.File(resultFile))
    val publicKey = Helpers.getPublicKey()

    val someone = new Paillier(publicKey)
    // denominator (sum(w_i)), numerator (sum(beta * w_i))
    var weightSum, betaWeightSum = someone.encrypt(BigInteger.ZERO)
    //for verification only
    var testWeightSum, testBetaWeightSum = 0.0
    var multiplier = 0.0

    val flushPerIterations = Helpers.property("flush_per_iterations").toInt
    writer.print("\"Inverse variance results\"")

    for ((line, indx) <- io.Source.fromFile(inputFile).getLines().zipWithIndex) {
      val record = line.split(",")

      if (indx == 0) {
        multiplier = record(1).toDouble
        writer.println(",Multiplier:," + record(1) + ",\"public key:\"," + publicKey)
      } else if (indx == 1) {
        writer.print(""""encrypted numerator","encrypted denominator"""")

        if (toVerify)
          writer.print(""","numerator(+)","numerator(-)",denominator,"expected numerator","expected denominator"""")

        writer.println
      } else {
        // Notes on input data: column #0: E(w_i); #1: E(beta*w_i, positive part); #2: E(beta*w_i, negative part)
        // to sum up w_i
        weightSum = someone.add(weightSum, new BigInteger(record(0)))
        val betaWeightI = someone.add( new BigInteger(record(1)),
          someone.multiply(new BigInteger(record(2)), BigInteger.valueOf(-1)) )
        betaWeightSum = someone.add(betaWeightSum, betaWeightI)
        // Output encryptions of numerator/denominator
        writer.print(betaWeightSum + "," + weightSum)

        //DEBUG for verification only
        if (toVerify) {
          writer.print("," + decryptData(betaWeightSum) + "," + decryptData(betaWeightSum, true) + "," + decryptData(weightSum))

          testWeightSum += record(3).toDouble
          testBetaWeightSum += record(4).toDouble
          writer.print("," + testBetaWeightSum * multiplier + "," + testWeightSum * multiplier)
        }

        writer.println

        // test with Secure ln(x)
        //val numerator = decryptedBetaWeightSum.multiply(decryptedBetaWeightSum)
      }

      //NOTE: this counter is inaccurate, as it won't imply specific end point
      if (indx % flushPerIterations == 0) print("\rRecords processded: ~" + indx)
    }

    writer.close()
    println(" Result saved in " + resultFile)
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
      coefficients(j) = constA.pow(powerI - j).multiply(BigInteger.valueOf(ArithmeticUtils.binomialCoefficient(powerI, j))).multiply(tmp)  //.mod(FieldMax)
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
   * @param encrypted encryption
   * @param negative whether the final result is going to be negative
   * @return plain value result
   */
  def decryptData(encrypted: BigInteger, negative: Boolean = false) = {
    val privateKeys = KeyGen.PaillierThresholdKeyLoad(new File(Helpers.property("data_directory"), Helpers.property("private_keys")).toString)
    val parties = for (k <- privateKeys.take(Helpers.property("threshold_parties").toInt)) yield new PaillierThreshold(k)
    val decrypted = parties(0).combineShares((for (p <- parties) yield p.decrypt(encrypted)): _*)

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

    for (variableI <- 2 to K_TAYLOR_PLACES) {
      val nextVector = polynomialCoefficients(constA, variableI)
      coefficients = for ((a, b) <- coefficients zip nextVector) yield a.add(b)  //NOTE: add mod() will cause problems
    }

    // Perform Taylor expansion (assemble coefficients and variables)
    //TODO read Alice's input via network
    //val encryptedPowers = MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Alice.power")
    val someone = new Paillier(Helpers.getPublicKey())

    //(encryptedPowers zip coefficients).foldLeft(someone.encrypt(BigInteger.ZERO)) ((a, x) => someone.add(a, someone.multiply(x._1, x._2)))
    val paillierN = someone.getPublicKey.getN
    var tmpResult = someone.encrypt(BigInteger.ZERO)  //.mod(someone.getPublicKey.getN)
    val nSquared = someone.getPublicKey.getN.multiply(someone.getPublicKey.getN)
    for (i <- 0 to encryptedPowers.size) {
      println("Before: " + tmpResult.compareTo(someone.getPublicKey.getN))
      val tmpMultiple = someone.multiply(encryptedPowers(i), coefficients(i))
      tmpResult = someone.add(tmpResult, tmpMultiple)
      println("after: " + tmpResult.compareTo(someone.getPublicKey.getN))
    }

    tmpResult
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

    //TODO transfer from socket
    val betas = (for (i <- Array("Alice", "Bob")) yield MyUtil.readResult(MyUtil.pathFile(FairplayFile) + "." + i + ".beta")(0))
    val tmp = someone.add(betas(0), betas(1))
    someone.add(taylorResult, tmp)
  }

  def decryptLn(encryptedLn: BigInteger, scale: Int = 6, negative: Boolean = false) = {
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


  // Scale-up and store beta
  //TODO move to common utils
  def storeBeta(who: String = "Bob", beta: BigInteger) = {
    val someone = new Paillier(Helpers.getPublicKey())
    var scaledBeta = someone.encrypt(BigInteger.valueOf(2).pow(Mediator.MaxN * (Mediator.K_TAYLOR_PLACES - 1)).multiply(BigInteger.valueOf(Mediator.LCM)).multiply(beta.abs))
    if (beta.compareTo(BigInteger.ZERO) < 0) scaledBeta = someone.multiply(scaledBeta, BigInteger.valueOf(-1))
    MyUtil.saveResult(Array[BigInteger](scaledBeta), MyUtil.pathFile(FairplayFile) + "." + who + ".beta")
  }


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

    //inverseVariance(Helpers.property("encrypted_data_file"), Helpers.property("final_result_file"), true)

    //--- Run Fairplay ---
    // Generate keys only when forced to or no keys exist
    if ( args.length > 0 && args(0).equals("init") || (! new File(Helpers.property("data_directory"), Helpers.property("private_keys")).exists()) ) {
      generateKeys()
      compile()
    }

    // Will store output to file
    val socketPort = Helpers.property("socket_port")
    Bob.main(Array("-r", Helpers.property("fairplay_script"), "dj2j", "4", socketPort))

    try {
      val Array(_, beta) = MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Bob.output").filter(_ != null)
      storeBeta("Bob", beta)
    } catch {
      case e: Exception => e.printStackTrace()
    }

    //--- Compute ln(x) ---
    val Array(alpha, beta) = MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Bob.output").filter(_ != null)
    println("Computed: " + actualLn(alpha, beta, 10))

    //getPublicKey()
    //inverseVariance()
    //distributeKeys()



    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000.0 + " seconds.")
  }
}