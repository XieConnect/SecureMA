package test

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/10/12
 */

import org.scalatest.FunSuite
import java.math.BigInteger
import main.{Mediator, Provider}

class ProviderSuite extends FunSuite {
  test("Provider correctly encrypts powers of variable") {
    for (variableA <- 1 to 3) {
      val encrypted = Provider.encryptPowers(BigInteger.valueOf(variableA))
      expect(Mediator.K_TAYLOR_PLACES + 1)(encrypted.size)

      val decrypted = encrypted.map(a => Mediator.decryptData(a))
      val constA = decrypted(1).divide(decrypted(0))
      for (i <- 1 to decrypted.size - 2) {
        assert(decrypted(i+1).divide(decrypted(i)) === constA)
      }
    }
  }

}