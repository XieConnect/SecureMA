package main

/**
 * @description Helper methods for project
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/30/12
 */

import java.io.{PrintWriter, File, FileInputStream}
import java.util.{Random, Properties}
import paillierp.key.{KeyGen, PaillierKey}
import java.math.BigInteger
import paillierp.{PaillierThreshold, Paillier}

object Helpers {
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
    writers(0).println(shareRand.negate())
    writers(0).println(2)  //TODO (rnd.nextInt(rndRange))
    writers(0).println(5)  //TODO (rnd.nextInt(rndRange))

    // input for party 2
    writers(1).println(xValue.add(shareRand))

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
}
