package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Data Owners encrypt and contribute their data
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 9/24/12
 */

import java.math.BigInteger
import java.io.{File, PrintWriter}
import scala.Array
import akka.actor._
import akka.routing.RoundRobinRouter

object Owner {
  val MULTIPLIER = Helpers.getMultiplier()

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


  sealed trait VerificationMessage
  case object Verify extends VerificationMessage
  case class Work(indx: Int, data: Array[String]) extends VerificationMessage
  case class Result(indx: Int, values: Array[Boolean]) extends VerificationMessage
  case class FinalResult(results: collection.mutable.Map[Int, Array[Boolean]])

  class Listener extends Actor {
    def receive = {
      case FinalResult(results) =>
        println("\n> Problematic records (no news is good news): ")
        results.filter(a => !(a._2(0) && a._2(1)) ).map(b =>
          println(" ERROR: #" + b._1 + ": " + b._2(0) + ", " + b._2(1)))
        context.system.shutdown()
    }
  }

  class Worker extends Actor {
    def receive = {
      case Work(indx, data) =>
        val weight_i_correct: Boolean = Mediator.decryptData(new BigInteger(data(0))) ==
          Helpers.toBigInteger(data(2).toDouble * MULTIPLIER)
        val beta_weight_correct: Boolean = Mediator.decryptData(new BigInteger(data(1)), (data(3).toDouble < 0)) ==
          Helpers.toBigInteger(data(3).toDouble * MULTIPLIER)

        sender ! Result( indx, Array(weight_i_correct, beta_weight_correct) )
    }
  }

  class Master(inputFile: String, listener: ActorRef) extends Actor {
    val lines = io.Source.fromFile(inputFile).getLines().drop(1).toArray
    var recordsProcessed: Int = _
    var results = collection.mutable.Map[Int, Array[Boolean]]()
    val totalRecords: Int = lines.size
    var numberOfCores = Helpers.property("total_cores").toInt
    if (numberOfCores < 1) numberOfCores = 1
    val workerRouter = context.actorOf(
      Props[Worker].withRouter(RoundRobinRouter(numberOfCores)), name = "workerRouter")

    def receive = {
      case Verify =>
        for ((line, indx) <- lines.view.zipWithIndex; record = line.split(",")) {
          workerRouter ! Work(indx, record)
        }

      case Result(indx, values) =>
        results += indx -> values
        recordsProcessed += 1
        println("  verify #" + indx + ": " + values(0) + ", " + values(1))
        if (recordsProcessed >= totalRecords) {
          listener ! FinalResult(results)
          context.stop(self)
        }
    }
  }

  /**
   * Verify correctness of encrypted data before submitting to others (in parallel)
   * @param encryptedFile file containing encryptions
   * @return true if no errors found
   */
  def verifyEncryption(encryptedFile: String = Helpers.property("encrypted_data_file")) = {
    //- Verify Paillier key size
    println("> To verify key size...")
    println("  Stored: " + Helpers.getPublicKey().getK + ";  " + " size in code: " + Mediator.FieldBitsMax)

    if (Helpers.getPublicKey().getK >= Mediator.FieldBitsMax) {
      println("  Key size correct")
    } else {
      println("  Key size ERROR!")
    }

    //- Verify encryption correctness
    val system = ActorSystem("VerificationSystem")
    val listener = system.actorOf(Props[Listener], name = "listener")
    val master = system.actorOf(Props(new Master(encryptedFile, listener)), name = "master")
    master ! Verify

  }

  /**
   * Perform Data Owners' roles (prepare/encrypt data)
   * @param args: if called with "verify-only", then only do verification on encrypted file;
   *              if with "verify", then do encryption and verification;
   *              otherwise, do encryption only.
   */
  def main(args: Array[String]) = {
    if (args.length > 0 && args(0).equals("verify-only")) {
      println( "> Begin verification: ")
      verifyEncryption()

    } else {
      prepareData( rawFile = Helpers.property("raw_data_file"),
        encryptedFile = Helpers.property("encrypted_data_file") )

      if (args.size > 0 && args(0).equals("verify")) {
        println( "> Begin verification: ")
        verifyEncryption()
      }

    }
  }
}
