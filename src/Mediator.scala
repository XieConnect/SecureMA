/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 5/24/12
 */

import java.io.ObjectOutputStream
import java.math.BigInteger
import java.net.ServerSocket
import java.util.Random
import org.apache.commons.math3.linear.{RealVector, ArrayRealVector}
import paillierp.Paillier
import paillierp.key.KeyGen
import paillierp.key.PaillierPrivateThresholdKey
import paillierp.key.PaillierKey

import paillierp.PaillierThreshold
import java.math.{BigDecimal, RoundingMode}

import org.apache.commons.math3.util.ArithmeticUtils

object Mediator {
  val K_TAYLOR_PLACES = 10
  val LCM = (2 to K_TAYLOR_PLACES).foldLeft(1)((a, x) => ArithmeticUtils.lcm(a, x))
  val POWER_OF_TWO = math.pow(2, 20)  //2^N

  /**
   * Read in public keys from file
   * @param inputFile
   * @return
   */

  /*
  def getPublicKey(inputFile: String = "data/public_key.bin") = {
    var buffer = new java.io.BufferedInputStream(new java.io.FileInputStream(inputFile))
    var input = new java.io.ObjectInputStream(buffer)
    input.readObject().asInstanceOf[PaillierKey]
  }
  */


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
    toParties.writeInt(333)  // send test data
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
    val tmp = new BigInteger("%.0f".format(math.pow(POWER_OF_TWO, K_TAYLOR_PLACES - powerI) * math.pow(-1, powerI - 1)))
    val multiplier = tmp.multiply(BigInteger.valueOf(LCM / powerI))

    for (j <- 0 to powerI) {
      coefficients(j) = constA.pow(powerI - j).multiply(BigInteger.valueOf(ArithmeticUtils.binomialCoefficient(powerI, j))).multiply(multiplier)
    }

    coefficients
  }


  def dotProduct(coefficients: Array[BigInteger], encryptedPowers: Array[BigInteger]) = {
    //TODO reduncancy
    val privateKeys = Provider.prepareData(toVerifyEncryption = false)
    val publicKey = privateKeys(0).getPublicKey
    val someone = new Paillier(publicKey)

    (for ((a, b) <- coefficients zip encryptedPowers) yield someone.multiply(b, a)).foldLeft(someone.encrypt(BigInteger.ZERO))((m,x) => someone.add(m, x))
  }


  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    //inverseVariance()
    //distributeKeys()

    var result = Array.fill[BigInteger](K_TAYLOR_PLACES + 1)(BigInteger.ZERO)
    for (variableI <- 1 to K_TAYLOR_PLACES) {
      val nextVector = polynomialCoefficients(new BigInteger("123456789123456789"), variableI)
      result = for ((a, b) <- result zip nextVector) yield a.add(b)
      println()
      println("Result: ")
      result.map(println)
      println
    }


    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000.0 + " seconds.")
  }
}