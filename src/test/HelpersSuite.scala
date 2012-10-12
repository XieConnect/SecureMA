package test

import org.scalatest.FunSuite
import main.{Experiment, Helpers}
import java.io.File
import paillierp.key.KeyGen
import paillierp.{PaillierThreshold, Paillier}
import java.math.BigInteger
import java.util.Random
import io.Source

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 9/24/12
 */
class HelpersSuite extends FunSuite {
  /*
  test("reads property from config file") {
    assert(Helpers.property("data_directory") != "")
  }

  test("prefix data_directory to queried file name") {
    expect(new File(Helpers.property("data_directory"), Helpers.property("public_keys")).toString) (
      Helpers.propertyFullPath("public_keys")
    )
  }

  //NOTE: please make sure keys have already been generated!
  test("keys files exist") {
    for (a <- Array("public_keys", "private_keys")) {
      expect(true) (new File(Helpers.propertyFullPath(a)).exists())
    }
  }

  test("outputs correct public key") {
    val privateKeys = KeyGen.PaillierThresholdKeyLoad(Helpers.propertyFullPath("private_keys"))
    val parties = for (k <- privateKeys.take(Helpers.property("threshold_parties").toInt)) yield new PaillierThreshold(k)
    val generator = new Random()

    val someone = new Paillier(Helpers.getPublicKey())
    // Test with multiple rand values
    for (_ <- 1 to 3) {
      val value = BigInteger.valueOf(generator.nextInt(Integer.MAX_VALUE))
      val encryption = someone.encrypt(value)
      val decryption = parties(0).combineShares((for (p <- parties) yield p.decrypt(encryption)): _*)
      expect(value) (decryption)
    }
  }
  */

  test("converts encryption to secret shares") {
    val someone = new Paillier(Helpers.getPublicKey())
    val rnd = new Random()
    for (_ <- 1 to 5) {
      val plainValue = BigInteger.valueOf(rnd.nextLong())
      var encryption = someone.encrypt(plainValue.abs)
      if (plainValue.compareTo(BigInteger.ZERO) < 0) encryption = someone.multiply(encryption, -1)

      val (share1, share2) = Helpers.encryption2Shares(encryption, plainValue)
      expect(0) (share1.add(share2).compareTo(plainValue))
    }
  }

  test("prepares correct inputs for Fairplay") {
    // test multiple times
    for (count <- 1 to 5) {
      val i = BigInteger.valueOf(new Random().nextInt(10000))
      Helpers.prepareInputs(i)
      val inputs = Array("Alice", "Bob").map( a =>
        new BigInteger(Source.fromFile(Experiment.PathPrefix + a + ".input").getLines().toArray.head)
      )

      expect(0) (inputs(0).add(inputs(1)).compareTo(i))
    }
  }

}
