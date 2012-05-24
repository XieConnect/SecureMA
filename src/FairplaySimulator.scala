/**
 * @description This program simulates Fairplay for ln x computation
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 5/16/12
 */

object FairplaySimulator {
  val N = 20  //should be upper bound for n
  val Factor = math.pow(2, N).toInt

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
  def getNAndEpsilon(share1: Int, share2: Int, randoms: (Int, Int)) = {
    val x = share1 + share2

    //estimate n (assuming no log() or multiply() operators provided)
    var tmp = 2
    var n = 1
    for (i <- 1 to N) {
      if (tmp < x) {
        //tmp would be equal to 2^n after loop
        tmp = tmp + tmp
        n = n + 1
      }
    }

    //estimate epsilon * 2^N
    tmp = x - tmp
    //scale up with 2^N
    for (i <- 1 to (N - n)) {
      tmp = tmp + tmp
    }

    //TODO hardcode ln 2
    val raisedln2 = (math.log(2) * math.pow(2, N)).toInt
    var nln2 = 0
    for (i <- 1 to n) {
      nln2 = nln2 + raisedln2
    }

    println("Estimate: " + n)

    //return epsilon*2^N and 2^N * n * ln 2
    //P1: alpha1, beta1;  P2: alpha2, beta2
    (tmp - randoms._1, nln2 - randoms._2)
  }


  //lcm(2, ..., k)
  def lcmK(k: Int) = {
    var tmp = 2
    for (i <- 3 to k) {
      tmp = lcm(tmp, i)
    }

    tmp
  }


  //TODO errors!  define Q(x) polynomial
  def qPolynomial(a1: Int, k: Int, random: Int, x: Int) = {
    val multiple = lcmK(k)
    var result = 0
    for (i <- 1 to k) {
      //TODO error
      result += lcmK / i
        math.pow(-1, i-1).toInt * math.pow(a1 + x, i).toInt * multiple / (math.pow(2, N*(i-1)).toInt * i)
    }

    (random, result - random)
  }


  /**
   * Entrance function
   */
  def main(args: Array[String]) = {
    val randoms = (1, 1)
    val (a1, b1) = randoms
    println("2^N = 2^" + N + " = " + Factor)

    val x = 10

    val (a2, b2) = getNAndEpsilon(2, x - 2, randoms)
    println("x= " + x)
    println("eps= " + (a1+a2).toDouble / Factor)

    val w1 = 0 //random
    val k = 14  //TODO define max k
    val (_, w2) = qPolynomial(a1, k, w1, a2)

    val lcmConstant = lcmK(k)
    val u1 = lcmConstant * b1 + w1
    val u2 = lcmConstant * b2 + w2

    println("Est: " + (u1+u2).toDouble / Factor / lcmConstant)
    println("Act: " + (math.log(x)))
    println()
  }
}