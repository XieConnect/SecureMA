package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Ensure encryption/decryption produces correct result
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 */

import org.scalatest.FunSuite
import java.math.BigInteger

class EncryptionSuite extends FunSuite {
  val someone = new paillierp.Paillier(Helpers.getPublicKey())
  val paillierN = someone.getPublicKey.getN

  def testSamples: Array[BigInteger] = Array("1", "377373", "2726266343").map(new BigInteger(_))

  test("encryption works correctly for numbers within field size") {
    for (a <- testSamples) {
      assert(a.compareTo(paillierN) < 0)
      expectResult(a)(Mediator.decryptData(someone.encrypt(a)))
    }
  }

  test("encryption rejects number larger than field size") {
    intercept[IllegalArgumentException] {
      someone.encrypt(paillierN.add(BigInteger.TEN))
    }
  }

  test("refuse to encrypt negative number") {
    intercept[IllegalArgumentException] {
      someone.encrypt(BigInteger.valueOf(-3))
    }
  }

  test("use tricks to encrypt negative numbers") {
    val paillierNSquared = someone.getPublicKey.getNSPlusOne

    for (a <- testSamples.map(_.negate())) {
      // this generates the original decryption of a negative value
      val encryption = Helpers.encryptNegative(someone, a)
      val decrypted = Mediator.decryptDataNoProcessing( encryption )
      assert(a.compareTo(decrypted) < 0)
      // should give correct result in "negated" form
      expectResult(a)(Mediator.decryptData( encryption ))
    }
  }

  test("decryptions are of expected bit length") {
    val samples = testSamples :+ paillierN.divide(BigInteger.valueOf(2))
    val paillierBits = paillierN.bitLength

    for (a <- samples) {
      assert(a.signum() > 0 && a.compareTo(paillierN) < 0)
      assert(paillierBits > Mediator.decryptData( someone.encrypt(a) ).bitLength)
    }

    // negative values
    for (a <- samples.map(_.negate())) {
      val decryption = Mediator.decryptDataNoProcessing( Helpers.encryptNegative(someone, a) )
      assert(paillierBits - decryption.bitLength <= 1)
    }
  }

  test("decrypt to yield negative values") {
    val samples = (testSamples :+ paillierN.divide(BigInteger.valueOf(2))).map(_.negate())
    for (a <- samples) {
      assert(a.signum < 0)
      expectResult(a)( Mediator.decryptData( Helpers.encryptNegative(someone, a) ) )
    }
  }
}
