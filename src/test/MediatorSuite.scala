package test

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/11/12
 */

import org.scalatest.FunSuite
import java.math.BigInteger
import main.Mediator

class MediatorSuite extends FunSuite {
  test("binomial coefficients array is of correct size") {
    for (powerSize <- 2 to 10) {
      val coefficients = Mediator.polynomialCoefficients(BigInteger.valueOf(1), powerSize)
      expect(Mediator.K_TAYLOR_PLACES + 1)(coefficients.size)
      // non-zero values
      expect(powerSize + 1)(coefficients.filter(_.compareTo(BigInteger.valueOf(0)) != 0).size)
    }
  }

  test("generate correct binomial coefficients") {
    for (powerSize <- 2 to 6) {
      val coefficients = Mediator.polynomialCoefficients(BigInteger.valueOf(1), powerSize)
      for (i <- 0 to powerSize / 2) ( assert(coefficients(i) === coefficients(powerSize - i)) )
    }
  }


}
