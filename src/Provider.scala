/**
 * For providers to preprocess and upload data
 */

import java.math.BigInteger
import java.util.Random
import paillierp.Paillier
import paillierp.key.KeyGen
import paillierp.key.PaillierPrivateThresholdKey
import paillierp.key.PaillierKey

import paillierp.PaillierThreshold
import java.math.{BigDecimal, RoundingMode}


object Provider {
  //TODO Read config file
  val MULTIPLIER: Double = math.pow(10, 10)
  val PartiesNumberThreshold = 3
  val EncryptedDataFile = "data/encrypted_data.csv"
  val Delimiter = ","


  /**
   * Save keys to file
   * @param privateKeys  private threshold keys for all parties
   */
  def saveKeys(privateKeys: Array[PaillierPrivateThresholdKey]) = {
    var buffer = new java.io.BufferedOutputStream(new java.io.FileOutputStream("data/private_keys.bin"))
    var output = new java.io.ObjectOutputStream(buffer)
    output.writeObject(privateKeys)
    output.close

    buffer = new java.io.BufferedOutputStream(new java.io.FileOutputStream("data/public_key.bin"))
    output = new java.io.ObjectOutputStream(buffer)
    output.writeObject(privateKeys(0).getPublicKey)
    output.close

    println("All keys saved to file.")
  }


  def decryptData(encryptedData: BigInteger, thresholdParties: Array[PaillierThreshold]): BigInteger = {
    thresholdParties(0).combineShares((for (p <- thresholdParties) yield p.decrypt(encryptedData)): _*)
  }

  def decryptData(encryptedData: String, thresholdParties: Array[PaillierThreshold]): BigInteger = {
    decryptData(new BigInteger(encryptedData), thresholdParties)
  }


  /**
   * Verify correctness of encryption
   * @param inputFile  file containing encrypted data (first two rows are header)
   * @param privateKeys
   */
  def verifyEncryption(inputFile: String, privateKeys: Array[PaillierPrivateThresholdKey]) = {
    var indx = 0
    var multiplier = 0.0
    val epsilon = 1.0 / MULTIPLIER
    val thresholdParties: Array[PaillierThreshold] = for (k <- privateKeys.take(PartiesNumberThreshold)) yield new PaillierThreshold(k)

    for (line <- io.Source.fromFile(inputFile).getLines()) {
      val record = line.split(",")

      if (indx == 0) {
        multiplier = record(1).toDouble
        println("Compare multiplier: " + (multiplier == MULTIPLIER) + "  " + multiplier + " " + MULTIPLIER)
      } else if (indx > 1) {
        println( "Weight:      " +
          ((record(3).toDouble - decryptData(record(0), thresholdParties).doubleValue / multiplier).abs <= epsilon))
        println( "Beta*Weight: " +
          (((decryptData(record(1), thresholdParties) subtract decryptData(record(2), thresholdParties)).doubleValue / multiplier - record(4).toDouble).abs <= epsilon ))
      }

      indx += 1
    }
  }


  /**
   * Prepare data from raw values
   * @param fileName  raw data file
   * @return
   */
  def prepareData(fileName: String = "data/raw_data_sorted.csv", toVerifyEncryption: Boolean = true) = {
    val writer = new java.io.PrintWriter(new java.io.File(EncryptedDataFile))
    // Input
    val lines = io.Source.fromFile(fileName).getLines.drop(1).toArray
    val NUMBER_OF_PARTIES = lines.length

    //TODO distribute key generation
    val privateKeys = KeyGen.PaillierThresholdKey(128, NUMBER_OF_PARTIES, PartiesNumberThreshold, (new Random).nextLong())
    println("# Number of keys generated: " + NUMBER_OF_PARTIES + "  Threshold: " + PartiesNumberThreshold)

    saveKeys(privateKeys)

    val publicKey = privateKeys(0).getPublicKey()
    val someone = new paillierp.Paillier(publicKey)

    writer.println("Multiplier:," + MULTIPLIER)
    writer.println(""""encrypted weight_i","encrypted positive beta*weight","encrypted negative beta*weight","weight_i","beta*weight","positive beta*weight","negative beta*weight"""")

    for (line <- lines; record = line.split(Delimiter)) {
      val weightI = 1.0 / math.pow(record(11).toDouble, 2)
      val betaWeight = record(10).toDouble * weightI
      
      val raisedWeightI = new BigInteger("%.0f".format(weightI * MULTIPLIER))
      val raisedBetaWeight = new BigInteger("%.0f".format(betaWeight * MULTIPLIER))

      val splitBetaWeight = Array(BigInteger.ZERO, BigInteger.ZERO)
      if (betaWeight >= 0) {
        splitBetaWeight(0) = raisedBetaWeight
      } else {
        splitBetaWeight(1) = raisedBetaWeight.abs
      }

      val encryptedWeight = someone.encrypt(raisedWeightI)
      val encryptedBetaWeightPositive = someone.encrypt(splitBetaWeight(0))
      val encryptedBetaWeightNegative = someone.encrypt(splitBetaWeight(1))
      writer.print( encryptedWeight + "," +
                    encryptedBetaWeightPositive + "," +
                    encryptedBetaWeightNegative + ",")

      // Store plain values
      writer.print(weightI + "," +
                   betaWeight + "," +
                   splitBetaWeight(0) + "," +
                   splitBetaWeight(1) + "\n")
    }

    writer.close()

    if (toVerifyEncryption) {
      verifyEncryption(EncryptedDataFile, privateKeys)
    }

    //TODO for test only. will move to file later
    privateKeys
  }



  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()

    prepareData()

    println("\nProcess finished in " + (System.currentTimeMillis - startedAt) / 1000.0 + " seconds.")
  }
}