package test

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/10/12
 */

import org.scalatest.FunSuite
import java.math.BigInteger
import main.{Mediator, Manager}
import java.util.Random

class ManagerSuite extends FunSuite {
  // help verify correctness of powers encryption (both positives and negatives)
  def verifyEncryptionCorrectness(negative: Boolean = false) = {
    val rand = new Random()
    // Run multiple random tests
    for (i <- 0 to 1) {
      var variableA = rand.nextInt(300)
      // generate negative cases
      if (negative) {
        variableA = - variableA
        expect(true) (variableA <= 0)
      } else {
        expect(true) (variableA >= 0)
      }

      val decrypted = Manager.encryptPowers(BigInteger.valueOf(variableA)).zipWithIndex.map {
        case (a, indx) => Mediator.decryptData(a, if (negative && indx % 2 == 1) true else false)
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
    //List(rand, - rand).foreach()
  }

  test("correctly encrypts powers of positives") {
    verifyEncryptionCorrectness(false)
  }

  test("correctly encrypts powers of negatives") {
    verifyEncryptionCorrectness(true)
  }

}
