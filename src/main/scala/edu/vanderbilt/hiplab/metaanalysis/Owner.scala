package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Data Owners encrypt and contribute their data
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 9/24/12
 */

import java.math.BigInteger
import java.io.{File, PrintWriter}
import scala.Array
import actors.Actor

object Owner {

  /**
   * Encrypt data for sharing (save encryptions in .csv file)
   * @param rawFile  path to file containing raw data
   * @param encryptedFile file to store encrypted data
   */
  def prepareData( rawFile: String = Helpers.property("raw_data_file"),
                  encryptedFile: String = Helpers.property("encrypted_data_file") ) = {
    val writer = new PrintWriter(new File(encryptedFile))
    val someone = new paillierp.Paillier(Helpers.getPublicKey())
    val paillierNS = someone.getPublicKey.getN.pow(2)

    //writer.println("Multiplier:," + Helpers.MULTIPLIER)
    writer.println(""""encrypted weight_i","encrypted beta*weight",weight_i,beta*weight,"experiment identifiers"""")
    var indexCount = 0
    // for how many rows will we flush buffer to file
    val flushPerIterations = Helpers.property("flush_per_iterations").toInt

    // ignore .csv header
    for (line <- io.Source.fromFile(rawFile).getLines.drop(1); record = line.split(",")) {
      val weightI = 1.0 / math.pow(record(11).toDouble, 2)  // = 1 / se^2
      val betaWeight = record(10).toDouble * weightI
      val raisedWeightI = Helpers.toBigInteger(weightI * Helpers.getMultiplier())
      val raisedBetaWeight = Helpers.toBigInteger(betaWeight.abs * Helpers.getMultiplier())

      // For beta * weight, submit both positive and negative parts
      //val splitBetaWeight = Array(BigInteger.ZERO, BigInteger.ZERO)

      var encryptedBetaWeight = someone.encrypt(raisedBetaWeight).mod(paillierNS)
      if (betaWeight < 0) encryptedBetaWeight = someone.multiply(encryptedBetaWeight, -1).mod(paillierNS)

      //if (betaWeight >= 0) {
        //splitBetaWeight(0) = raisedBetaWeight
      //} else {
        //splitBetaWeight(1) = raisedBetaWeight.abs
      //}

      //writer.print(someone.encrypt(raisedWeightI) + "," +
      //  someone.encrypt(splitBetaWeight(0)) + "," +
      //  someone.encrypt(splitBetaWeight(1)) + ",")
      writer.println(someone.encrypt(raisedWeightI).mod(paillierNS) + "," + encryptedBetaWeight
                   + "," + weightI + "," + betaWeight
                   + "," + (record.slice(0, 4) ++ record.slice(5, 10)).mkString(",") )

      indexCount += 1

      if (indexCount % flushPerIterations == 0) {
        writer.flush()
        print("\r Records processed: " + indexCount)
      }
    }

    println
    writer.close()

    println(">> Encrypted data saved in: " + encryptedFile)
  }

  /**
   * Verify correctness of encrypted data before submitting to others (in parallel)
   * @param encryptedFile file containing encryptions
   * @return true if no errors found
   */
  def verifyEncryption(encryptedFile: String = Helpers.property("encrypted_data_file")): Boolean = {
    val lines = io.Source.fromFile(encryptedFile).getLines().toArray
    var rowsCorrect = true
    val multiplier = Helpers.getMultiplier()

    //- Verify Paillier key size
    println("> To verify key size...")
    println("  Stored: " + Helpers.getPublicKey().getK + ";  " + " size in code: " + Mediator.FieldBitsMax)

    if (Helpers.getPublicKey().getK >= Mediator.FieldBitsMax) {
      println("  Key size correct")
    } else {
      println("  Key size ERROR!")
    }

    //- Verify encryption correctness
    println("> To verify encryptions...")
    // Verify record row by row
    for ((line, indx) <- lines.drop(1).view.zipWithIndex; record = line.split(",")) {
      new Actor {
        override def act() = {
          print("Row # " + indx + " : ")

          // Verify weight_i (note it's non-negative)
          if (Mediator.decryptData(new BigInteger(record(0))) != Helpers.toBigInteger(record(2).toDouble * multiplier)) {
            print("  weight_i error!!; ")
            rowsCorrect = false
          }

          val decryptedBetaWeight = Mediator.decryptData(new BigInteger(record(1)), (record(3).toDouble < 0))
          if (decryptedBetaWeight != Helpers.toBigInteger(record(3).toDouble * multiplier)) {
            print("  beta*weight error!!")
            rowsCorrect = false
          }
          println
        }
      }.start

    }

    rowsCorrect
  }

  /**
   * Perform Data Owners' roles (prepare/encrypt data)
   * @param args: if called with "verify-only", then only do verification on encrypted file;
   *              if with "verify", then do encryption and verification;
   *              otherwise, do encryption only.
   */
  def main(args: Array[String]) = {
    if (args.length > 0 && args(0).equals("verify-only")) {
      println( "> Verification result: " + verifyEncryption() )

    } else {
      prepareData( rawFile = Helpers.property("raw_data_file"),
        encryptedFile = Helpers.property("encrypted_data_file") )

      if (args.size > 0 && args(0).equals("verify"))
        println( "> Verification result: " + verifyEncryption() )
    }
  }
}
