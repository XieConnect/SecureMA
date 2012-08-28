package main

/**
 * Data Providers pre-process and upload data
 * Final data will be stored in file specified by EncryptedDataFile below
 */

import java.io.{ObjectOutputStream, ObjectInputStream}
import java.math.BigInteger
import java.net.Socket
import java.util.Random
import paillierp.Paillier
import paillierp.key.KeyGen
import paillierp.key.PaillierPrivateThresholdKey
import paillierp.key.PaillierKey

import paillierp.PaillierThreshold
import java.math.{BigDecimal, RoundingMode}

import SFE.BOAL.{MyUtil, Alice}

object Provider {
  //TODO Read config file
  val MULTIPLIER: Double = math.pow(10, 10)
  // to hard-code if needed
  val PartiesNumberThreshold = 3
  val EncryptedDataFile = "data/encrypted_data.csv"
  val Delimiter = "," // how is raw data file delimited

  var FairplayFile = "progs/Sub.txt"

  /**
   * Receive private keys from the mediator
   */
  //TODO store data in file securely and easy to restore
  def receiveKey() = {
    val socket = new Socket("localhost", 3497)
    val fromOrigin = new ObjectInputStream(socket.getInputStream())
    println("Received: " + fromOrigin.readInt())

    socket.close()
    fromOrigin.close()
  }


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


  /**
   * For testing only
   * @param encryptedData
   * @param thresholdParties
   * @return
   */
  def decryptData(encryptedData: BigInteger, thresholdParties: Array[PaillierThreshold]): BigInteger = {
    thresholdParties(0).combineShares((for (p <- thresholdParties) yield p.decrypt(encryptedData)): _*)
  }

  def decryptData(encryptedData: String, thresholdParties: Array[PaillierThreshold]): BigInteger = {
    decryptData(new BigInteger(encryptedData), thresholdParties)
  }


  /**
   * Verify correctness of encryption (for testing)
   * @param inputFile  file containing encrypted data (first two rows are header)
   * @param privateKeys  private keys
   * @return  number of errors for Weight and Beta*Weight
   */
  def verifyEncryption(inputFile: String, privateKeys: Array[PaillierPrivateThresholdKey]) = {
    var indx = 0
    var multiplier = 0.0
    val epsilon = 1.0 / MULTIPLIER
    val thresholdParties: Array[PaillierThreshold] = for (k <- privateKeys.take(PartiesNumberThreshold)) yield new PaillierThreshold(k)
    var weightErrors, betaWeightErrors = 0

    for (line <- io.Source.fromFile(inputFile).getLines()) {
      val record = line.split(Delimiter)

      if (indx == 0) {
        multiplier = record(1).toDouble
        println("Compare multiplier: " + (multiplier == MULTIPLIER) + "  " + multiplier + " " + MULTIPLIER)
      } else if (indx > 1) {
        val compareWeight = ((record(3).toDouble - decryptData(record(0), thresholdParties).doubleValue / multiplier).abs <= epsilon)
        val compareBetaWeight = (((decryptData(record(1), thresholdParties) subtract decryptData(record(2), thresholdParties)).doubleValue / multiplier - record(4).toDouble).abs <= epsilon)

        if (!compareWeight) {
          weightErrors += 1
        }
        if (!compareBetaWeight) {
          betaWeightErrors += 1
        }

        println("Weight:      " + compareWeight)
        println("Beta*Weight: " + compareBetaWeight)
      }

      indx += 1
    }

    println("All verification finished. Weight: [" + weightErrors + "] errors;  Beta*Weight: [" + betaWeightErrors + "] errors.")

    (weightErrors, betaWeightErrors)
  }


  /**
   * Prepare (encrypted) data for sharing
   * @param fileName  path to file containing raw data
   * @param toVerifyEncryption  if set to true, will decrypt to verify encryption (for testing)
   * @return  all private keys (for dev only)
   */
  def prepareData(fileName: String = "data/raw_data_sorted.csv", toVerifyEncryption: Boolean = true) = {
    // to store encrypted data
    val writer = new java.io.PrintWriter(new java.io.File(EncryptedDataFile))
    // Input: file row contains labels
    val lines = io.Source.fromFile(fileName).getLines.drop(1).toArray


    val someone = new paillierp.Paillier(Mediator.getPublicKey())

    writer.println("Multiplier:" + Delimiter + MULTIPLIER)
    // TODO use Delimiter instead of ","
    writer.println( """"encrypted weight_i","encrypted positive beta*weight","encrypted negative beta*weight","weight_i","beta*weight","positive beta*weight","negative beta*weight"""")

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
      writer.print(encryptedWeight + Delimiter +
        encryptedBetaWeightPositive + Delimiter +
        encryptedBetaWeightNegative + Delimiter)

      // Store plain values (for testing)
      writer.print(weightI + Delimiter +
        betaWeight + Delimiter +
        splitBetaWeight(0) + Delimiter +
        splitBetaWeight(1) + "\n")
    }

    writer.close()

    /*
    if (toVerifyEncryption) {
      verifyEncryption(EncryptedDataFile, privateKeys)
    }

    println("Saved encrypted data in: " + EncryptedDataFile)

    //TODO for test only. will move to file later
    privateKeys
    */
  }


  /**
   * Run Alice (communicate with Bob)
   * TODO read filename from config
   */
  def runAlice() = {
    Alice.main(Array("-r", FairplayFile, "djdj", "localhost"))
    MyUtil.readResult(MyUtil.pathFile(FairplayFile) + ".Alice.output").filter(_ != null).asInstanceOf[Array[BigInteger]]
  }


  /**
   * Compute encryptions of powers of alpha2
   * @param baseValue  alpha2
   * @return  vector of encrypted powers
   */
  def encryptPowers(baseValue: BigInteger) = {
    val someone = new Paillier(Mediator.getPublicKey())
    val powers = Array.fill[BigInteger](Mediator.K_TAYLOR_PLACES + 1)(BigInteger.ZERO)

    //TODO read size K (of places) from shared config
    (0 to Mediator.K_TAYLOR_PLACES).map { i =>
        val t = baseValue.pow(i);
        if (t.signum() >= 0) someone.encrypt(t) else someone.multiply(someone.encrypt(t.abs()), BigInteger.valueOf(-1))
    }.toArray
  }


  def sendData(powers: Array[BigInteger], beta: BigInteger) = {
    val ss = new Socket("localhost", 3497);
    val os = new ObjectOutputStream(ss.getOutputStream())
    os.writeObject(powers)
    os.writeObject(beta)
    os.flush()

    os.close()
    ss.close()
  }


  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    //Mediator.generateKeys()
    //prepareData()
    //receiveKey()


    FairplayFile = "progs/Sub.txt"

    val Array(alpha, beta) = runAlice()

    val encryptedPowers = encryptPowers(alpha)
    //TODO send to Mediator via network
    MyUtil.saveResult(encryptedPowers, MyUtil.pathFile(FairplayFile) + ".Alice.power")

    //println("Provider: about to send data...")
    //sendData(encryptedPowers, beta)
    //println("Provider: finished send data...")

    Mediator.storeBeta("Alice", beta)



    println("\nProcess finished in " + (System.currentTimeMillis - startedAt) / 1000.0 + " seconds.")
  }
}