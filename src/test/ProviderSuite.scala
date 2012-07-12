package test

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/10/12
 */

import org.scalatest.FunSuite
import io.Source
import SFE.BOAL.MyUtil
import java.math.BigInteger

class ProviderSuite extends FunSuite {
  //TODO config file path
  val PathPrefix = "run/progs/Sub.txt."

  def readInputs() = {
    val bobInput = Source.fromFile(PathPrefix + "Bob.input").getLines().map(Integer.parseInt).toArray
    val aliceInput = Source.fromFile(PathPrefix + "Alice.input").getLines().map(Integer.parseInt).toArray

    (aliceInput, bobInput)
  }

  //TODO config file path
  def readOutputs() = {
    val aliceOutput = MyUtil.readResult(PathPrefix + "Alice.output").filter(_ != null).asInstanceOf[Array[BigInteger]]
    val bobOutput = MyUtil.readResult(PathPrefix + "Bob.output").filter(_ != null).asInstanceOf[Array[BigInteger]]

    (aliceOutput, bobOutput)
  }


  test("Fairplay inputs are in correct form") {
    // bounded to specific input formats
    val (aliceInput, bobInput) = readInputs()

    expect(3)(bobInput.size)
    expect(1)(aliceInput.size)
  }

  test("Fairplay outputs are in correct form") {
    val (aliceOutput, bobOutput) = readOutputs()
    expect(2)(aliceOutput.size)
    expect(2)(bobOutput.size)
  }

  test("Fairplay gives correct result") {
    val PowerOfN = math.pow(2, 20)

    val (aliceInput, bobInput) = readInputs()
    val (aliceOutput, bobOutput) = readOutputs()

    val x = aliceInput(0) + bobInput(0)
    val n = (math.log(x) / math.log(2)).ceil.toInt

    // ensure epsilon to be within error range
    val expectedEpsilon = new BigInteger("%.0f".format(PowerOfN * (x / math.pow(2, n) - 1)))
    val epsilonError = aliceOutput(0).add(bobOutput(0)).subtract(expectedEpsilon).divide(new BigInteger("%.0f".format(PowerOfN))).abs().compareTo(BigInteger.valueOf(1))
    assert(epsilonError <= 0)

    // ensure n*ln 2 to be within error range
    val expectedNln2 = new BigInteger("%.0f" format n * math.log(2) * PowerOfN)
    val nln2Error = aliceOutput(1).add(bobOutput(1)).subtract(expectedNln2).divide(new BigInteger("%.0f".format(PowerOfN))).abs().compareTo(BigInteger.ONE)
    assert(nln2Error < 0)
  }

}
