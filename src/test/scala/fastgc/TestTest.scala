package fastgc

import java.math.BigInteger
import edu.vanderbilt.hiplab.securema.Mediator

/**
  * Refer to README for details.
  * Author: Wei Xie
  * Version:
  */
object TestTest {
   def main(args: Array[String]) = {

//     for (i <- 0 to 30) {
//       val a = new CircuitQuery(3491)
//       val b = new CircuitQuery(3492)
//
//       println("Iteration # " + i)
//       val aa = future { a.query(BigInteger.valueOf(i - 25)) }
//       val bb = future { b.query(BigInteger.valueOf(25)) }
//
//       for (result <- Await.result(aa, 20 seconds)) println(result)
//       for (result <- Await.result(bb, 20 seconds)) println(result)
//     }

     println(Mediator.lnWrapper(BigInteger.valueOf(10)))
     println(Mediator.lnWrapper(BigInteger.valueOf(21)))
     //println(Mediator.lnWrapper(BigInteger.valueOf(10), bobPort = 3491, alicePort = 3492))


   }
 }
