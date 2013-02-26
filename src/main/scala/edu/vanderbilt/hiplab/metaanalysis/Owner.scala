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

  sealed trait MyMessage
  case object Encrypt extends MyMessage
  case object Verify extends MyMessage
  case class VerificationWork(indx: Int, data: Array[String]) extends MyMessage
  case class EncryptionWork(record: Array[String], someone: paillierp.Paillier,
                            paillierNS: BigInteger) extends MyMessage
  case class VerificationResult(indx: Int, values: Array[Boolean]) extends MyMessage
  case class EncryptionResult(result: String) extends MyMessage
  case class VerificationResults(results: collection.mutable.Map[Int, Array[Boolean]])
  case class EncryptionResults()

  // Report final result
  class ResultReport extends Actor {
    def receive = {
      case EncryptionResults() =>
        println("\n> Finished writing encryptions to file.")
        context.system.shutdown()

      case VerificationResults(results) =>
        println("\n> Problematic records (no news is good news): ")
        results.filter(a => !(a._2(0) && a._2(1)) ).map(b =>
          println(" ERROR: #" + b._1 + ": " + b._2(0) + ", " + b._2(1)))
        context.system.shutdown()
    }
  }

  // Slave workers
  class Worker extends Actor {
    def receive = {
      case EncryptionWork(record, someone, paillierNS) =>
        val weightI = 1.0 / math.pow(record(11).toDouble, 2)  // = 1 / se^2
        val betaWeight = record(10).toDouble * weightI
        val raisedWeightI = Helpers.toBigInteger(weightI * Helpers.getMultiplier())
        val raisedBetaWeight = Helpers.toBigInteger(betaWeight.abs * Helpers.getMultiplier())
        var encryptedBetaWeight = someone.encrypt(raisedBetaWeight).mod(paillierNS)
        if (betaWeight < 0) encryptedBetaWeight = someone.multiply(encryptedBetaWeight, -1).mod(paillierNS)

        sender ! EncryptionResult(someone.encrypt(raisedWeightI).mod(paillierNS) + "," + encryptedBetaWeight
          + "," + weightI + "," + betaWeight
          + "," + (record.slice(0, 4) ++ record.slice(5, 10)).mkString(",") )

      case VerificationWork(indx, data) =>
        val weight_i_correct: Boolean = Mediator.decryptData(new BigInteger(data(0))) ==
          Helpers.toBigInteger(data(2).toDouble * MULTIPLIER)
        val beta_weight_correct: Boolean = Mediator.decryptData(new BigInteger(data(1)), (data(3).toDouble < 0)) ==
          Helpers.toBigInteger(data(3).toDouble * MULTIPLIER)

        sender ! VerificationResult( indx, Array(weight_i_correct, beta_weight_correct) )
    }
  }

  class Master(inputFile: String, resultReporter: ActorRef) extends Actor {
    var recordsProcessed: Int = _
    var verificationResults = collection.mutable.Map[Int, Array[Boolean]]()
    var totalRecords: Int = _
    var numberOfCores = Helpers.property("total_cores").toInt
    if (numberOfCores < 1) numberOfCores = 1
    val workerRouter = context.actorOf(
      Props[Worker].withRouter(RoundRobinRouter(numberOfCores)), name = "workerRouter")
    var encryptionWriter: PrintWriter = _

    def receive = {
      case Encrypt =>
        val lines = io.Source.fromFile(Helpers.property("raw_data_file")).getLines.drop(1).toArray
        totalRecords = lines.size
        val someone = new paillierp.Paillier(Helpers.getPublicKey())
        val paillierNS = someone.getPublicKey.getN.pow(2)
        encryptionWriter = new PrintWriter(new File(Helpers.property("encrypted_data_file") ))
        encryptionWriter.println("encrypted_weight_i,encrypted_beta*weight,weight_i,beta*weight,experiment_identifiers")

        for (line <- lines; record = line.split(",")) {
          workerRouter ! EncryptionWork(record, someone, paillierNS)
        }

      case Verify =>
        val lines = io.Source.fromFile(inputFile).getLines().drop(1).toArray
        totalRecords = lines.size
        for ((line, indx) <- lines.view.zipWithIndex; record = line.split(",")) {
          workerRouter ! VerificationWork(indx, record)
        }

      case EncryptionResult(result) =>
        recordsProcessed += 1
        print("\rRecords processed: " + recordsProcessed)
        encryptionWriter.println(result)
        if (recordsProcessed >= totalRecords) {
          if (encryptionWriter != null) {
            encryptionWriter.flush()
            encryptionWriter.close()
          }
          resultReporter ! EncryptionResults()
          context.stop(self)
        }

      case VerificationResult(indx, values) =>
        verificationResults += indx -> values
        recordsProcessed += 1
        println("  verify #" + indx + ": " + values(0) + ", " + values(1))
        if (recordsProcessed >= totalRecords) {
          resultReporter ! VerificationResults(verificationResults)
          context.stop(self)
        }
    }
  }

  /**
   * Encrypt data for sharing (save encryptions in .csv file)
   * @param rawFile  path to file containing raw data
   * @param encryptedFile file to store encrypted data
   */
  def prepareData( rawFile: String = Helpers.property("raw_data_file"),
                  encryptedFile: String = Helpers.property("encrypted_data_file") ) = {

    //- Verify encryption correctness
    val system = ActorSystem("EncryptionSystem")
    val resultReporter = system.actorOf(Props[ResultReport], name = "resultReporter")
    val master = system.actorOf(Props(new Master(encryptedFile, resultReporter)), name = "master")
    master ! Encrypt

    println(">> Encrypted data saved in: " + encryptedFile)
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
    val listener = system.actorOf(Props[ResultReport], name = "listener")
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
