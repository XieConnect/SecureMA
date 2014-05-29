package edu.vanderbilt.hiplab.securema

import org.scalatest.FunSuite
import java.math.BigInteger

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/19/12
 */

class SimulatorSuite extends FunSuite {
  val PowerOfN = math.pow(2, FairplaySimulator.MaxN)

  test("verify Fairplay") {
    val aliceInput = 3
    val bobInput = (6, 2, 5)
    val x = aliceInput + bobInput._1
    val n = (math.log(x) / math.log(2)).ceil.toInt

    // ensure epsilon to be within error range
    val expectedEpsilon = new BigInteger("%.0f".format(PowerOfN * (x / math.pow(2, n) - 1)))

    val (aliceOutputs, bobOutputs) = FairplaySimulator.fairplay(aliceInput, bobInput)
    val epsilonError = aliceOutputs._1.add(bobOutputs._1).subtract(expectedEpsilon).divide(new BigInteger("%.0f".format(PowerOfN))).abs().compareTo(BigInteger.valueOf(1))
    println(epsilonError <= 0)

    // ensure n*ln 2 to be within error range
    val expectedNln2 = new BigInteger("%.0f" format n * math.log(2) * PowerOfN)
    val nln2Error = aliceOutputs._2.add(bobOutputs._2).subtract(expectedNln2).divide(new BigInteger("%.0f".format(PowerOfN))).abs().compareTo(BigInteger.ONE)
    assert(nln2Error < 0)
  }
}
