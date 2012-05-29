/**
 * @description This program simulates Fairplay for ln x computation
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 5/16/12
 */

import java.math.{BigInteger, BigDecimal}


object FairplaySimulator {
  val N = 20  //should be upper bound for n
  //TODO hard-code into Fairplay
  val Factor = new BigInteger("%.0f".format(math.pow(2, N)))

  def log2(x: Int) = {
    math.log(x) / math.log(2.0)
  }


  def gcd(a: Int, b: Int):Int = if (b==0) a.abs else gcd(b, a%b)
  def lcm(a: Int, b: Int) = (a*b).abs/gcd(a,b)


  /**
   * @param share1  one share of number x
   * @param share2  the other share of number x
   * @param randoms  two random numbers provided to blind return result
   * @return  Two-party shares for n and epsilon
   */
  def getNAndEpsilon(share1: BigInteger, share2: BigInteger, randoms: (BigInteger, BigInteger)) = {
    val x = share1.add(share2)

    //estimate n (assuming no log() or multiply() operators provided)
    var tmp = BigInteger.valueOf(2)
    var n: Int = 1
    for (i <- 1 to N) {
      if (tmp.compareTo(x) == -1) {
        //tmp would be equal to 2^n after loop
        tmp = tmp.add(tmp)
        n = n + 1
      }
    }

    //estimate epsilon * 2^N
    tmp = x.subtract(tmp)
    //scale up with 2^N  (we already scaled with 2^n)
    for (i <- 1 to (N - n)) {
      tmp = tmp.add(tmp)
    }

    //TODO hardcode 2^N * ln 2
    val raisedln2 = new BigInteger("%.0f".format(math.log(2) * math.pow(2, N)))
    //compute n * (2^N * ln 2)
    var nln2 = BigInteger.ZERO
    for (i <- 1 to n) {
      nln2 = nln2.add(raisedln2)
    }

    println("Estimate of n: " + n)

    //return epsilon*2^N and 2^N * n * ln 2
    //P1: alpha1, beta1;  P2: alpha2, beta2
    (tmp.subtract(randoms._1), nln2.subtract(randoms._2))
  }


  /**
   * Least-common-multiple: lcm(2, ..., k)
   * It should work well under Int32 range
   * TODO use reference table in Fairplay
   */
  def lcmK(k: Int) = {
    var tmp = 2
    for (i <- 3 to k) {
      tmp = lcm(tmp, i)
    }

    BigInteger.valueOf(tmp)
  }


  //TODO define Q(x) polynomial
  def qPolynomial(a1: BigInteger, k: Int, random: BigInteger, z: BigInteger) = {
    val multiple = lcmK(k)
    var result = BigInteger.ZERO
    var numerator = BigInteger.valueOf(-1)
    var factorTmp = BigInteger.valueOf(1)

    for (i <- 1 to k) {
      numerator = BigInteger.ZERO.subtract( numerator.multiply(a1.add(z)) )
      result = result.add(numerator.divide( factorTmp.multiply(multiple.divide(BigInteger.valueOf(i))) ))
      factorTmp = factorTmp.multiply(Factor)
    }

    result.subtract(random)
  }


  /**
   * Entrance function
   */
  def main(args: Array[String]) = {
    val startedAt = System.currentTimeMillis()


    val randoms = (BigInteger.valueOf(1), BigInteger.valueOf(1))
    val (a1, b1) = randoms
    println("2^N = 2^" + N + " = " + Factor)

    val share1 = BigInteger.valueOf(2)
    val share2 = BigInteger.valueOf(18)

    //TODO remove
    val x = share1.add(share2)

    val (a2, b2) = getNAndEpsilon(share1, share2, randoms)
    println("x= " + x)
    println("eps= " + new BigDecimal(a1.add(a2)).divide(new BigDecimal(Factor)))


    val z1 = BigInteger.valueOf(3) //random
    val k = 20  //TODO define max k
    val z2= qPolynomial(a1, k, z1, a2)

    val multiple = lcmK(k)
    val u1 = z1.add(multiple.multiply(b1))
    val u2 = z2.add(multiple.multiply(b2))

    println("Est: " + new BigDecimal(u1.add(u2)).divide(new BigDecimal(Factor)).divide(new BigDecimal(multiple)))
    println("Act: " + (math.log(x.doubleValue())))

    println("\nProcess finished in " + (System.currentTimeMillis() - startedAt)/1000.0 + " seconds.")
  }
}