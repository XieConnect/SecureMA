package main

/**
 * @description Data Owners encrypt and contribute their data
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 9/24/12
 */

import java.math.BigInteger
import scala.Array

object Owner {
  val MULTIPLIER: Double = math.pow(10, 10)

  /**
   * Encrypt data for sharing
   * @param fileName  path to file containing raw data
   * @param encryptedFile file to store encrypted data
   */
  def prepareData(fileName: String = Helpers.property("raw_data_file"),
                  encryptedFile: String = Helpers.property("encrypted_data_file")) = {
    val writer = new java.io.PrintWriter(new java.io.File(encryptedFile))
    val someone = new paillierp.Paillier(Helpers.getPublicKey())

    writer.println("Multiplier:," + MULTIPLIER)
    writer.println(""""encrypted weight_i","encrypted positive beta*weight","encrypted negative beta*weight",""" +
        """"weight_i","beta*weight","positive beta*weight","negative beta*weight"""")

    for (line <- io.Source.fromFile(fileName).getLines.drop(1); record = line.split(",")) {
      val weightI = 1.0 / math.pow(record(11).toDouble, 2)
      val betaWeight = record(10).toDouble * weightI
      val raisedWeightI = Helpers.toBigInteger(weightI * MULTIPLIER)
      val raisedBetaWeight = Helpers.toBigInteger(betaWeight * MULTIPLIER)

      // For beta * weight, submit both positive and negative parts
      val splitBetaWeight = Array(BigInteger.ZERO, BigInteger.ZERO)
      if (betaWeight >= 0) {
        splitBetaWeight(0) = raisedBetaWeight
      } else {
        splitBetaWeight(1) = raisedBetaWeight.abs
      }

      writer.print(someone.encrypt(raisedWeightI) + "," +
        someone.encrypt(splitBetaWeight(0)) + "," +
        someone.encrypt(splitBetaWeight(1)) + ",")

      // Store plain values (for testing)
      writer.print(weightI + "," +
        betaWeight + "," +
        splitBetaWeight(0) + "," +
        splitBetaWeight(1))

      writer.print("\n")
    }

    writer.close()

    println(">> Encrypted data saved in: " + encryptedFile)
  }

  /**
   * Verify correctness of encrypted data before submitting to others
   * @param encryptedFile file containing encryptions
   * @return true if no errors found
   */
  def verifyEncryption(encryptedFile: String = Helpers.property("encrypted_data_file")): Boolean = {
    val lines = io.Source.fromFile(encryptedFile).getLines().toArray
    var multiplierCorrect = true
    var rowsCorrect = true

    if (lines(0).split(",")(1).toDouble != MULTIPLIER)
      multiplierCorrect = false

    for ((line, indx) <- lines.drop(2).view.zipWithIndex if multiplierCorrect == true; record = line.split(",")) {
      //verify weight_i
      if (Mediator.decryptData(new BigInteger(record(0))) != Helpers.toBigInteger(record(3).toDouble * MULTIPLIER)) {
        println("  weight_i error in row " + indx)
        rowsCorrect = false
      }

      if (Mediator.decryptData(new BigInteger(record(1))) != new BigInteger(record(5))) {
        println("  positive beta*weight error in row " + indx)
        rowsCorrect = false
      }

      if (Mediator.decryptData(new BigInteger(record(2))) != new BigInteger(record(6))) {
        println("  negative beta*weight error in row " + indx)
        rowsCorrect = false
      }
    }

    multiplierCorrect && rowsCorrect
  }

  def main(args: Array[String]) = {
    prepareData()
    //verifyEncryption()
  }
}
