package edu.vanderbilt.hiplab.metaanalysis

/**
 * @description Refer to README
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/11/12
 */

import org.scalatest.FunSuite
import java.io.File

class MediatorSuite extends FunSuite {
  // We only test if keys are generated and stored. Correctness of keys are checked in HelpersSuite
  test("generates and stores keys") {
    Mediator.generateKeys()
    assert(new File(Helpers.propertyFullPath("private_keys")).exists())
    assert(new File(Helpers.propertyFullPath("public_keys")).exists())
  }

  //NOTE: this test take time!
  test("inverse-variance computes numerator and denominator correctly") {
    //We need to re-encrypt data, as keys may have changed since last time
    Owner.prepareData(Helpers.property("raw_data_file"), Helpers.property("encrypted_data_file"))
    //Mediator.inverseVariance(Helpers.property("encrypted_data_file"), Helpers.property("final_result_file"), true)

    var multiplier = 0.0
    for ((line, indx) <- io.Source.fromFile(Helpers.property("final_result_file")).getLines().zipWithIndex; record = line.split(",")) {
      if (indx == 0) {
        multiplier = record(2).toDouble
      } else if (indx > 1) {
        val expectedNumerator = record(5).toDouble
        val expectedDenominator = record(6).toDouble
        val computedNumerator = (if (expectedNumerator >= 0) record(2) else record(3)).toDouble

        //Ensure computed numerator/denominator fall into expected error range
        assert( (computedNumerator - expectedNumerator).abs / multiplier  < 0.01)
        assert( (record(4).toDouble - expectedDenominator).abs / multiplier  < 0.01)
      }
    }
  }

  /*
  //TODO config file path
  val PathPrefix = "run/progs/Sub.txt."

  def readInputs() = {
    val bobInput = Source.fromFile(PathPrefix + "Bob.input").getLines().map(_.toLong).toArray
    val aliceInput = Source.fromFile(PathPrefix + "Alice.input").getLines().map(_.toLong).toArray

    (aliceInput, bobInput)
  }

  //TODO config file path
  def readOutputs() = {
    val aliceOutput = MyUtil.readResult(PathPrefix + "Alice.output").filter(_ != null).asInstanceOf[Array[BigInteger]]
    val bobOutput = MyUtil.readResult(PathPrefix + "Bob.output").filter(_ != null).asInstanceOf[Array[BigInteger]]

    (aliceOutput, bobOutput)
  }


  test("Mediator and Manager run correctly") {
    AutomatedTest.main(Array())
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

  //TODO delete. for dev only
  test("decryption works correctly") {
    val pubkey = Helpers.getPublicKey()
    val someone = new Paillier(pubkey)

    // there is hacking involved in getting negatives to work in Paillier
    for (i <- -3 to 3) {
      var encrypted = someone.encrypt(BigInteger.valueOf(i.abs))
      if (i < 0) encrypted = someone.multiply(encrypted, BigInteger.valueOf(-1))
      var decrypted = Mediator.decryptData(encrypted)
      if (i < 0) decrypted = decrypted.subtract(pubkey.getN)

      assert(BigInteger.valueOf(i) === decrypted)
    }
  }

  /* TODO taylorExpansion
  test("verify Taylor expansion result") {
    val (_, bobOutput) = readOutputs()
    val taylorResult = Mediator.taylorExpansion(bobOutput(0))
    // NOTE: work-around to get negatives from decrypted result
    //TODO debug
    val result = Mediator.decryptData(taylorResult).subtract(Mediator.getPublicKey().getN)

    val divisor = new BigInteger("%.0f" format Mediator.POWER_OF_TWO).pow(Mediator.K_TAYLOR_PLACES).multiply(BigInteger.valueOf(Mediator.LCM))
    expect(BigInteger.ZERO) (result.divide(divisor))
  }
  */

  test("verify final result") {
    val (aliceInput, bobInput) = readInputs()
    val (_, bobOutput) = readOutputs()
    val expected = math.log(aliceInput(0) + bobInput(0))

    assert((Mediator.actualLn(bobOutput(0), bobOutput(1)).doubleValue() - expected).abs <= 0.001)
  }
  */

}
