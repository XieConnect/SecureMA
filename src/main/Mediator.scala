package main

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 5/24/12
 */

import java.io.ObjectOutputStream
import java.math.BigInteger
import java.net.ServerSocket
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

object Mediator {
  val K_TAYLOR_PLACES = 10
  val LCM = (2 to K_TAYLOR_PLACES).foldLeft(1)((a, x) => ArithmeticUtils.lcm(a, x))
  val MaxN = 20
  val POWER_OF_TWO = math.pow(2, MaxN)
  //2^N
  val FieldBitsMax = (MaxN + 2) * K_TAYLOR_PLACES + (math.log(MaxN) / math.log(2)).ceil.toInt
  val FieldMax = new BigInteger("%.0f".format(math.pow(2, FieldBitsMax)))

  var FairplayFile = "progs/Sub.txt"


  /**
   * Generate and store Paillier Threshold keys to file
   * @param privateKeyFile
   * @param publicKeyFile
   * @param length
   * @param totalParties
   * @param thresholdParties
   * @param seed
   */
  def generateKeys(privateKeyFile: String = "data/private.keys", publicKeyFile: String = "data/public.keys", length: Int = FieldBitsMax,
                   totalParties: Int = 6, thresholdParties: Int = 3, seed: Long = new util.Random().nextLong()) = {

    // Generate and store private keys
    KeyGen.PaillierThresholdKey(privateKeyFile, length, totalParties, thresholdParties, seed)

    // Store public key
    val publicKey = KeyGen.PaillierThresholdKeyLoad(privateKeyFile)(0).getPublicKey()
    val buffer = new java.io.BufferedOutputStream(new java.io.FileOutputStream(publicKeyFile))
    val output = new java.io.ObjectOutputStream(buffer)
    output.writeObject(publicKey)
    output.close
    buffer.close()

    (privateKeyFile, publicKeyFile)
  }


  /**
   * Read in public keys from file
   * TODO remove verifyPublicKey
   * @param filename  file storing public key
   * @param verifyPublicKey  whether to verify correctness of public key or not
   * @return
   */
  def getPublicKey(filename: String = "data/public.keys", verifyPublicKey: Boolean = false) = {
    val buffer = new java.io.BufferedInputStream(new java.io.FileInputStream(filename))
    val input = new java.io.ObjectInputStream(buffer)
    val publicKey = input.readObject().asInstanceOf[PaillierKey]

    input.close()
    buffer.close()

    // To verify that public key is correct
    //TODO for debug only
    if (verifyPublicKey) {
      val tmp = BigInteger.valueOf(10)
      var encryptedTmp = BigInteger.ZERO

      for (i <- 1 to 2) {
        if (i == 1) {
          val someone = new Paillier(publicKey)
          encryptedTmp = someone.encrypt(tmp)
        } else {
          val privateKeys = KeyGen.PaillierThresholdKeyLoad("data/private.keys")
          val parties = for (k <- privateKeys.take(3)) yield new PaillierThreshold(k)
          println(">> Public Key Correctness: " + tmp.equals(parties(0).combineShares((for (p <- parties) yield p.decrypt(encryptedTmp)): _*)))
        }
      }
    }

    publicKey
  }


  /**
   * Inverse-variance (Effect-size) based approach for Meta-analysis
   * @param inputFile  file containing encrypted data
   */
  def inverseVariance(inputFile: String = "data/encrypted_data.csv") = {
    //var publicKey = getPublicKey()

    // Read in private keys
    //var buffer = new java.io.BufferedInputStream(new java.io.FileInputStream("data/private_keys.bin"))
    //var input = new java.io.ObjectInputStream(buffer)
    //val keys = input.readObject().asInstanceOf[Array[PaillierPrivateThresholdKey]].slice(0, 5)

    //TODO read from file (below is a work-around)
    val privateKeys = Provider.prepareData(toVerifyEncryption = false)
    val publicKey = privateKeys(0).getPublicKey

    println("Keys loaded. Now start SMC...")

    val someone = new Paillier(publicKey)
    var weightSum, betaWeightSum = someone.encrypt(BigInteger.ZERO)
    //for verification only
    var testWeightSum, testBetaWeightSum = 0.0

    val thresholdParties: Array[PaillierThreshold] = for (k <- privateKeys.take(Provider.PartiesNumberThreshold)) yield new PaillierThreshold(k)

    var indx = 0
    var multiplier = 0.0

    for (line <- io.Source.fromFile(inputFile).getLines()) {
      val record = line.split(Provider.Delimiter)

      if (indx == 0) {
        multiplier = record(1).toDouble
      } else if (indx > 1) {
        weightSum = someone.add(weightSum, new BigInteger(record(0)))
        val betaWeightI = someone.add(new BigInteger(record(1)), someone.multiply(new BigInteger(record(2)), BigInteger.valueOf(-1)))
        betaWeightSum = someone.add(betaWeightSum, betaWeightI)

        val decryptedWeightSum = Provider.decryptData(weightSum, thresholdParties)
        val decryptedBetaWeightSum = Provider.decryptData(betaWeightSum, thresholdParties)
        val computedResult = decryptedBetaWeightSum.multiply(decryptedBetaWeightSum).doubleValue() / (decryptedWeightSum).doubleValue() / multiplier

        //for result verification only
        testBetaWeightSum += record(4).toDouble
        testWeightSum += record(3).toDouble
        val expectedResult = math.pow(testBetaWeightSum, 2) / testWeightSum
        println(computedResult)
        println(expectedResult)
        println()
      }

      indx += 1
    }

    println("Totally " + indx + " records.")
  }


  //TODO move to separate class
  //TODO send actual keys; config port and hostname, etc
  /**
   * Distribute private keys to participants
   */
  def distributeKeys() = {
    val ss = new ServerSocket(3497)
    println("Host waiting...")
    val socket = ss.accept()
    //TODO convert to array of streams
    val toParties = new ObjectOutputStream(socket.getOutputStream())
    toParties.writeInt(333) // send test data
    toParties.flush()

    toParties.close()
    socket.close()
    ss.close()
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


  def dotProduct(coefficients: Array[BigInteger], encryptedPowers: Array[BigInteger]) = {
    //TODO reduncancy
    val privateKeys = Provider.prepareData(toVerifyEncryption = false)
    val publicKey = privateKeys(0).getPublicKey
    val someone = new Paillier(publicKey)

    (for ((a, b) <- coefficients zip encryptedPowers) yield someone.multiply(b, a)).foldLeft(someone.encrypt(BigInteger.ZERO))((m, x) => someone.add(m, x))
  }


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
    Bob.main(Array("-r", "progs/Sub.txt", "dj2j", "4"))
    MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Bob.output").filter(_ != null).asInstanceOf[Array[BigInteger]]
  }


  def decryptData(encrypted: BigInteger, negative: Boolean = false) = {
    val privateKeys = KeyGen.PaillierThresholdKeyLoad("data/private.keys")
    val parties = for (k <- privateKeys.take(3)) yield new PaillierThreshold(k)
    var decrypted = parties(0).combineShares((for (p <- parties) yield p.decrypt(encrypted)): _*)

    if (negative) decrypted.subtract(privateKeys(0).getN) else decrypted
  }


  /**
   * Phase 2 of secure ln(x): Taylor expansion
   * @param constA  alpha1 as in binomial expansion
   * @return  encrypted result of Taylor expansion
   */
  def taylorExpansion(constA: BigInteger) = {
    // Prepare constant coefficients
    var coefficients = polynomialCoefficients(constA, 1)

    for (variableI <- 2 to K_TAYLOR_PLACES) {
      val nextVector = polynomialCoefficients(constA, variableI)
      coefficients = for ((a, b) <- coefficients zip nextVector) yield a.add(b)  //NOTE: add mod() will cause problems
    }

    // Perform Taylor expansion (assemble coefficients and variables)
    //TODO read Alice's input via network
    val encryptedPowers = MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Alice.power")
    val someone = new Paillier(getPublicKey())

    (encryptedPowers zip coefficients).foldLeft(someone.encrypt(BigInteger.ZERO)) ((a, x) => someone.add(a, someone.multiply(x._1, x._2)))
  }


  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    generateKeys()
    //getPublicKey()
    //inverseVariance()
    //distributeKeys()

    // Run Fairplay
    FairplayFile = "progs/Sub.txt"

    compile()

    //TODO actually can discard return values, as they're the same as input from Bob
    val Array(alpha, beta) = runBob()

    Thread.sleep(5000) // wait for Alice to finish post-processing

    val taylorResult = taylorExpansion(alpha)


    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000.0 + " seconds.")
  }
}