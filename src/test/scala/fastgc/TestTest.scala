package fastgc

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit._
import java.math.BigInteger

/**
  * Refer to README for details.
  * Author: Wei Xie
  * Version:
  */
object TestTest {
   def main(args: Array[String]) = {
     val a = new CircuitQuery(3491)
     val b = new CircuitQuery(3492)

     for (i <- 0 to 5) {
       println("Iteration # " + i)
       val aa = future { a.query(BigInteger.valueOf(1 + i)) }
       val bb = future { b.query(BigInteger.valueOf(5 + i)) }

       for (result <- Await.result(aa, 20 seconds)) println(result)
       for (result <- Await.result(bb, 20 seconds)) println(result)
     }

   }
 }
