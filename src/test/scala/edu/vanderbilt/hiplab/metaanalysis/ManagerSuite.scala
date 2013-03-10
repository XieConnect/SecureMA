package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 */

import org.scalatest.FunSuite
import java.math.BigInteger
import java.util.Random

class ManagerSuite extends FunSuite {
  // helper to verify encryption correctness of powers (positive/negative)
  def verifyEncryptionCorrectness(negative: Boolean = false) = {
    val rand = new Random()
    // Run multiple random tests
    for (i <- 0 to 1) {
      var variableA = rand.nextInt(300)
      assert(variableA >= 0)

      // generate negative case
      if (negative) variableA = - variableA

      val decrypted = Manager.encryptPowers(BigInteger.valueOf(variableA)).zipWithIndex.map {
        case (a, indx) => Mediator.decryptData(a)
      }

      // to test if array is composed of powers of current variable
      for (i <- 0 to decrypted.size - 1) {
        expect(decrypted(1).pow(i)) (decrypted(i))
      }
    }
  }

  // If Taylor expands to k-th place, then there should be k+1 terms encrypted: x^0, ..., x^k
  test("encrypts exact number of powers") {
    val rand = new Random().nextInt(100)
    val encrypted = Manager.encryptPowers(BigInteger.valueOf(rand))
    expect(Mediator.K_TAYLOR_PLACES + 1)(encrypted.size)
  }

  test("encrypted powers are within Paillier range") {
    // get rid of 0
    val rand = new Random().nextInt(300) + 1
    assert(rand >= 0)

    val paillierNSquared = Helpers.getPublicKey().getN.pow(2)

    // test both positive and negative base values
    List(rand, - rand).foreach { a =>
      val encrypted = Manager.encryptPowers(BigInteger.valueOf(a))
      expect(true) (encrypted.forall(b => b.compareTo(paillierNSquared) <= 0))
    }
  }

  test("correctly encrypts powers of POSITIVE base value") {
    verifyEncryptionCorrectness(false)
  }

  test("correctly encrypts powers of NEGATIVE base value") {
    verifyEncryptionCorrectness(true)
  }

}
