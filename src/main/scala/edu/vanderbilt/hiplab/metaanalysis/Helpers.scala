package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Helper methods for project
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/30/12
 */

import java.io._
import java.util.{Random, Properties}
import paillierp.key.PaillierKey
import java.math.BigInteger
import paillierp.Paillier
import SFE.BOAL.MyUtil
import io.Source
import scala.Tuple2
import org.apache.commons.math3.util.ArithmeticUtils

object Helpers {
  var isPrecomputed = false

  // scaling factor during SMC
  val MyProperties = new Properties()
  val rand = new Random()
  var K_TAYLOR_PLACES = 0
  var MaxN = 0
  var LCM = BigInteger.ZERO
  var LN_DIVISOR = BigInteger.ZERO
  var POWER_OF_TWO = BigInteger.ZERO
  var FieldBitsMax = 0
  // used to scale-up n (gamma's in paper)
  var nScalingFactor = BigInteger.ZERO

  // pre-compute common constants
  def precompute() {
    if (! isPrecomputed) {
      K_TAYLOR_PLACES = Helpers.property("k_taylor_places").toInt  //it seems 7 is the cap
      MaxN = Helpers.property("max_exponent_n").toInt
      POWER_OF_TWO = ArithmeticUtils.pow(BigInteger.valueOf(2), MaxN)
      LCM = BigInteger.valueOf( (2 to K_TAYLOR_PLACES).foldLeft(1)((a, x) => ArithmeticUtils.lcm(a, x)) )
      LN_DIVISOR = ArithmeticUtils.pow(POWER_OF_TWO, K_TAYLOR_PLACES).multiply(LCM)
      nScalingFactor = new BigInteger("837963523372001241319907").multiply(
        BigInteger.valueOf(2).pow(MaxN * (K_TAYLOR_PLACES - 1) ).multiply(LCM) )
      FieldBitsMax = ((MaxN + 2) * K_TAYLOR_PLACES +
        (math.log(MaxN) / math.log(2) + math.log(Helpers.getMultiplier()  * 100) / math.log(2)).ceil.toInt)
    }

    isPrecomputed = true
  }

  /**
   * Find value corresponding to queried property in system config
   * @param key property name
   * @return value corresponding to the queried property
   */
  def property(key: String) = {
    val in = new FileInputStream("conf.properties")
    MyProperties.load(in)
    in.close()
    MyProperties.getProperty(key)
  }

  /**
   * Return multiple values for given keys (avoid redundant config file loading)
   * @param keys a series of keys
   * @return ArrayBuffer containing desired values
   */
  def properties(keys: String*) = {
    val in = new FileInputStream("conf.properties")
    MyProperties.load(in)
    in.close()
    keys.map(MyProperties.getProperty(_))
  }

  /**
   * Add data-directory prefix to queried file path
   * We don't like the overhead introduced by new File(bla1, bla2).toString
   * @param key property name
   * @return file path with data-directory prefixed
   */
  def propertyFullPath(key: String) = {
    new File(property("data_directory"), property(key)).toString
  }

  /**
   * SMC multiplier (10^param)
   * @return SMC multiplier
   */
  def getMultiplier(): Double = {
    math.pow(10, property("multiplier").toInt)
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
  def prepareInputs(xValue: BigInteger): Array[String] = {
    //val writers = Array("Bob", "Alice").map(a => new PrintWriter(new File(Experiment.PathPrefix + a + ".input")))
    //val shareRand = BigInteger.valueOf(3)  //TODO use real rand like: rnd.nextInt(rndRange)
    // input for party 1
    //writers(0).println(shareRand.negate())
    //writers(0).println(shareRand)
    //writers(0).println(2)  //TODO (rnd.nextInt(rndRange))
    //writers(0).println(5)  //TODO (rnd.nextInt(rndRange))

    // input for party 2
    //writers(1).println(xValue.subtract(shareRand))

    //writers.map(a => a.close())

    val shareRand = BigInteger.valueOf(rand.nextInt(Integer.MAX_VALUE))

    // bob, alice
    Array(shareRand + "\n" + BigInteger.valueOf(rand.nextInt()) + "\n" + BigInteger.valueOf(rand.nextInt()),
      xValue.subtract(shareRand).toString)
  }

  def randomizeInputs(xValue: BigInteger): Array[BigInteger] = {
    val shareRand = BigInteger.valueOf(rand.nextInt(Integer.MAX_VALUE))
    // bob, alice
    Array(shareRand, xValue.subtract(shareRand))
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
    val share1 = Mediator.decryptData(someone.add(encryption, encryptedRandom))
    val share2 = shareRand.negate()

    writers(0).println(share2)
    writers(0).println(2)  //TODO (rnd.nextInt(rndRange))
    writers(0).println(5)  //TODO (rnd.nextInt(rndRange))

    writers(1).println(share1)
    writers.map(a => a.close())

    (share1, share2)
  }

  def readFairplayResult(path: String): Array[BigInteger] = {
    var result = Array[BigInteger]()

    for (line <- Source.fromFile(path).getLines()) {
      if (line.startsWith("output")) {
        val values = line.trim.split("\\s")
        result = result :+ new BigInteger(values(1))
      }
    }

    result
  }

  def readTemporaryResult(path: String) = {
    val fin = new FileInputStream(path)
    val in = new ObjectInputStream(fin)
    val arr: Array[BigInteger] = in.readObject().asInstanceOf[Array[BigInteger]]
    in.close()
    fin.close()

    arr
  }

  /**
   * Read out stored Fairplay outputs
   * @param whichParty whose output to read out? (either Alice or Bob)
   * @return  shares of Alpha, and Beta
   */
  def getFairplayResult(whichParty: String = "Bob"): Array[BigInteger] = {
    // path of the form: run/progs/Sub.txt.Alice.output
    readFairplayResult(MyUtil.pathFile(property("fairplay_script")) + "." + whichParty + ".output").filter(_ != null)
  }

  def saveFairplayResult(result: Array[BigInteger], path: String) = {
    val out = new ObjectOutputStream(new FileOutputStream(path))

    try {
      out.writeObject(result)
      out.flush()
    } catch {
      case e: IOException => e.printStackTrace()
    } finally {
      out.close()
    }

  }

  /**
   * Scale-up and encrypt beta
   * Scaling factor from 2^N increased to 2^(Nk) * lcm(2,...,k)
   * @param beta  the originally scaled-up Beta output by Fairplay
   * @return encryption of scaled-up beta
   */
  def encryptBeta(beta: BigInteger, someone: Paillier = new Paillier(Helpers.getPublicKey())) = {
    val scaledBeta = someone.encrypt( nScalingFactor.multiply(beta.abs) )
    // handle negatives separately
    if (beta.compareTo(BigInteger.ZERO) < 0) someone.multiply(scaledBeta, BigInteger.valueOf(-1))  else scaledBeta
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
      new BigInteger(Source.fromFile(Experiment.PathPrefix + a + ".input").getLines().take(1).mkString)
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
    getPublicKey().getNSPlusOne
  }

  def encryptNegative(someone: Paillier, negativeValue: BigInteger): BigInteger = {
    someone.multiply(someone.encrypt(negativeValue.abs), -1).mod(someone.getPublicKey.getNSPlusOne)
  }

  def encryptData(someone: Paillier, rawData: BigInteger): BigInteger = {
    val tmp = someone.encrypt(rawData.abs)

    if (rawData.compareTo(BigInteger.ZERO) >= 0)  tmp else someone.multiply(tmp, -1).mod(someone.getPublicKey.getNSPlusOne)
  }
}
