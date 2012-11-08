package main

/**
 * @description Helper methods for project
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/30/12
 */

import java.io.{FileOutputStream, PrintWriter, File, FileInputStream}
import java.util.{Random, Properties}
import paillierp.key.{KeyGen, PaillierKey}
import java.math.BigInteger
import paillierp.{PaillierThreshold, Paillier}
import SFE.BOAL.MyUtil
import io.Source

object Helpers {
  // scaling factor during SMC
  val MULTIPLIER: Double = math.pow(10, 10)
  val MyProperties = new Properties()

  /**
   * Find value corresponding to queried property in system config
   * @param key property name
   * @return value corresponding to the queried property
   */
  def property(key: String) = {
    MyProperties.load(new FileInputStream("conf.properties"))
    MyProperties.getProperty(key)
  }

  /**
   * Add data-directory prefix to queried file path
   * We don't like the overhead introduced by new File(bla1, bla2).toString
   * @param key property name
   * @return file path with data-directory prefixed
   */
  def propertyFullPath(key: String) = {
    MyProperties.load(new FileInputStream("conf.properties"))
    MyProperties.getProperty("data_directory") + System.getProperty("file.separator") +
      MyProperties.getProperty(key)
  }

  /**
   * Read in public keys from file
   * @param filename  file storing public key
   * @return
   */
  def getPublicKey(filename: String = "") = {
    val filename_ = if (filename.equals("")) {
      new File(Helpers.property("data_directory"), Helpers.property("public_keys")).toString
    } else {
      filename
    }

    val buffer = new java.io.BufferedInputStream(new java.io.FileInputStream(filename_))
    val input = new java.io.ObjectInputStream(buffer)
    val publicKey = input.readObject().asInstanceOf[PaillierKey]

    input.close()
    buffer.close()

    publicKey
  }

  // Convert Double to BigInteger
  def toBigInteger(value: Double) = new BigInteger("%.0f" format value)

  /**
   * Simulate input shares generation for Fairplay
   * @param xValue  the x value as in ln(x)
   * @return  creates .input files
   */
  def prepareInputs(xValue: BigInteger) = {
    val writers = Array("Bob", "Alice").map(a => new PrintWriter(new File(Experiment.PathPrefix + a + ".input")))
    val shareRand = BigInteger.valueOf(3)  //TODO use real rand like: rnd.nextInt(rndRange)
    // input for party 1
    //writers(0).println(shareRand.negate())
    writers(0).println(shareRand)
    writers(0).println(2)  //TODO (rnd.nextInt(rndRange))
    writers(0).println(5)  //TODO (rnd.nextInt(rndRange))

    // input for party 2
    writers(1).println(xValue.subtract(shareRand))

    writers.map(a => a.close())
  }

  /**
   * Convert Paillier encryption to secret shares
   * For E(x), we return (in plain value): x1 = x + r1, x2 = - r1
   * TODO: it's cheating in dealing with negatives
   * @param encryption  Paillier encrypted input to be randomized
   */
  def encryption2Shares(encryption: BigInteger, plainValue: BigInteger): Tuple2[BigInteger, BigInteger] = {
    val writers = Array("Bob", "Alice").map(a => new PrintWriter(new File(Experiment.PathPrefix + a + ".input")))
    val shareRand = BigInteger.valueOf(new Random().nextInt(10000))
    val someone = new Paillier(getPublicKey())

    var encryptedRandom = someone.encrypt(shareRand.abs)
    if (shareRand.compareTo(BigInteger.ZERO) <0) encryptedRandom = someone.multiply(encryptedRandom, -1)

    // need to determine whether output is to be negative
    val shareNegative = plainValue.add(shareRand).compareTo(BigInteger.ZERO) < 0
    val share1 = Mediator.decryptData(someone.add(encryption, encryptedRandom), negative = shareNegative)
    val share2 = shareRand.negate()

    writers(0).println(share2)
    writers(0).println(2)  //TODO (rnd.nextInt(rndRange))
    writers(0).println(5)  //TODO (rnd.nextInt(rndRange))

    writers(1).println(share1)
    writers.map(a => a.close())

    (share1, share2)
  }

  /**
   * Read out stored Fairplay outputs
   * @param whichParty whose output to read out? (either Alice or Bob)
   * @return  shares of Alpha, and Beta
   */
  def getFairplayResult(whichParty: String = "Bob"): Array[BigInteger] = {
    // path of the form: run/progs/Sub.txt.Alice.output
    MyUtil.readResult(MyUtil.pathFile(property("fairplay_script")) + "." + whichParty + ".output").filter(_ != null)
  }

  /**
   * Further scale-up and store beta
   * Scaling factor from 2^N increased to 2^(Nk) * lcm(2,...,k)
   * @param who  can either be "Bob" or "Alice"
   * @param beta  the originally scaled-up Beta output by Fairplay
   */
  def storeBeta(who: String = "Bob", beta: BigInteger) = {
    val someone = new Paillier(Helpers.getPublicKey())
    var scaledBeta = someone.encrypt(BigInteger.valueOf(2).pow(Mediator.MaxN * (Mediator.K_TAYLOR_PLACES - 1)).multiply(BigInteger.valueOf(Mediator.LCM)).multiply(beta.abs))
    // handle negatives separately
    if (beta.compareTo(BigInteger.ZERO) < 0) scaledBeta = someone.multiply(scaledBeta, BigInteger.valueOf(-1))
    MyUtil.saveResult(Array[BigInteger](scaledBeta), MyUtil.pathFile(property("fairplay_script")) + "." + who + ".beta")
  }


  // for DEBUG only
  // Simulate specialized Fairplay script and verify output
  // NOTE need customization for different cases (over-, under- and optimal-estimate)
  def simulateFairplay() = {
    // Need to be consistent with current Fairplay script
    val MaxN = property("max_exponent_n").toInt
    val Nln2 = toBigInteger(math.pow(2, MaxN) * math.log(2))  // = 2^N * ln 2

    val pathPrefix = MyUtil.pathFile(property("fairplay_script"))
    val Array(aliceInput: Array[BigInteger], bobInput: Array[BigInteger]) = for (who <- Array("Alice", "Bob")) yield
      Source.fromFile(pathPrefix + "." + who + ".input").getLines().map(a => new BigInteger(a)).toArray

    // estimate n (as in x = 2^n) by simulating Fairplay script
    // needs to customize depending on whether it's under- or over-estimate
    var nEstimate = 0
    var tmpEstimate = BigInteger.ONE
    val xInput = aliceInput(0).add(bobInput(0))
    while (tmpEstimate.compareTo(xInput) <= 0) {
      tmpEstimate = tmpEstimate.add(tmpEstimate)
      nEstimate += 1
    }

    // epsilon
    tmpEstimate = xInput.subtract(tmpEstimate)
    // compute alpha = epsilon * 2^N
    for (i <- 1 to MaxN - nEstimate) (tmpEstimate = tmpEstimate.multiply(BigInteger.valueOf(2)))

    val beta = BigInteger.valueOf(nEstimate).multiply(Nln2)

    // Output simulation result (expected values)
    println("> Expected Fairplay results:")
    println("Alice.a = " + tmpEstimate.subtract(bobInput(1)))
    println("Alice.b = " + beta.subtract(bobInput(2)))
    println("Bob.a = " + bobInput(1))
    println("Bob.b = " + bobInput(2))

    // Output actual running result
    println("\n> Fairplay actual running result:")
    getFairplayResult("Alice").map(println)
    getFairplayResult("Bob").map(println)
  }

  def getPlainInput(): BigInteger = {
    val inputs = Array("Alice", "Bob").map(a =>
      new BigInteger(Source.fromFile(Experiment.PathPrefix + a + ".input").getLine(1))
    )

    inputs(0).add(inputs(1))
  }

  def copyFiles() = {
    for (a <- Array("conf.properties", MyUtil.pathFile(Helpers.property("fairplay_script")))) {
      val filename = a.substring(a.lastIndexOf("/") + 1)
      new FileOutputStream(Helpers.property("data_directory") + "/" + filename) getChannel() transferFrom(
        new FileInputStream(a) getChannel(), 0, Long.MaxValue
        )
    }
  }

  def paillierNS() = {
    getPublicKey().getN.pow(2)
  }

  def main(args: Array[String]) = {
    simulateFairplay()
  }
}
