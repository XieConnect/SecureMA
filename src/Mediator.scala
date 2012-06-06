/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 5/24/12
 */

import java.math.BigInteger
import java.util.Random
import paillierp.Paillier
import paillierp.key.KeyGen
import paillierp.key.PaillierPrivateThresholdKey
import paillierp.key.PaillierKey

import paillierp.PaillierThreshold
import java.math.{BigDecimal, RoundingMode}

object Mediator {
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


  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    inverseVariance()


    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt) / 1000.0 + " seconds.")
  }
}