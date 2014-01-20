package edu.vanderbilt.hiplab.metaanalysis

/**
 * Data Managers pre-process and submit encrypted data for computation
 * It runs Alice
 */

import java.io.{File, ObjectOutputStream, ObjectInputStream}
import java.math.BigInteger
import java.net.Socket
import paillierp.Paillier
import paillierp.key.PaillierPrivateThresholdKey

import paillierp.PaillierThreshold

import SFE.BOAL.{MyUtil, Alice}

object Manager {
  //TODO Read config file
  val MULTIPLIER: Double = math.pow(10, 10)
  // to hard-code if needed
  val PartiesNumberThreshold = 3
  // experiment results
  val EncryptedDataFile = "data/encrypted_data.csv"
  val Delimiter = "," // how is raw data file delimited

  var FairplayFile = Helpers.property("fairplay_script")

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
   * Run Alice (communicate with Bob)
   * @return  Alice's shares of alpha and beta
   */
  def runAlice() = {
    val socketServer = Helpers.property("socket_server")
    val socketPort = Helpers.property("socket_port")
    Alice.main(Array("-r", Helpers.property("fairplay_script"), "djdj", socketServer, socketPort)).filter(_ != null)

    //Helpers.getFairplayResult("Alice")
  }

  /**
   * Compute encryptions of powers of alpha2 (applicable to both positive/negative)
   * @param baseValue  alpha2
   * @return  vector of encrypted powers
   */
  def encryptPowers(baseValue: BigInteger): Array[BigInteger] = {
    val someone = new Paillier(Helpers.getPublicKey())
    val paillierN = someone.getPublicKey.getN
    val paillierNSquared = paillierN.multiply(paillierN)
    val kTaylorPlaces = Helpers.K_TAYLOR_PLACES

    val baseEncryption = someone.encrypt(BigInteger.ONE)

    if (baseValue.pow(kTaylorPlaces).abs.compareTo(paillierN) > 0) {
      println("!!! Error. Taylor power LARGER than Paillier field size")
      sys.exit()
    }

    (0 to kTaylorPlaces).par.map ( i =>
      someone.multiply(baseEncryption, baseValue.pow(i)).mod(paillierNSquared)).toArray
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

/*
    val powerFile = MyUtil.pathFile(FairplayFile) + ".Alice.power"

    // first need to delete previous .power file
    try {
      new File(powerFile).delete()
    } catch {
      case ex: Exception => println("Error in deleting .power file!")
    }
    */

//    var inputArgs = Array("-r", Helpers.property("fairplay_script"), "djdj", Helpers.property("socket_server"))
//    if (args.length > 1) {
//      // customize socket port
//      inputArgs :+= (Helpers.property("socket_port").toLong + (if (args.length > 2) args(2).toInt else 0)).toString
//      inputArgs :+= args(1)
//    }
//
//    val Array(alpha, beta) = Alice.main(inputArgs).filter(_ != null)


    //Helpers.storeBeta("Alice", beta)
//
//    val encryptedPowers = encryptPowers(alpha)
//
//    var socket: Socket = null
//    var connected = false
//    while (!connected) {
//      try {
//        socket = new Socket("localhost", inputArgs(4).toInt + 1)
//        connected = true
//      } catch { case e: Exception =>
//        e.printStackTrace()
//        Thread.sleep(70)
//      }
//    }
//
//    val os = socket.getOutputStream
//    val oos = new ObjectOutputStream(os)
//    try {
//      //println("[Alice] beta: " + beta)
//      //println("[Alice] powers: " + encryptedPowers.mkString("  "))
//      oos.writeObject(encryptedPowers)
//      oos.writeObject(Helpers.encryptBeta(beta))
//
//    } catch { case e: Exception =>
//      e.printStackTrace()
//    } finally {
//      oos.close()
//      os.close()
//      socket.close()
//    }

    println("\nManager finished in " + (System.currentTimeMillis - startedAt) / 1000.0 + " seconds.")
  }
}