package fastgc

import java.net.Socket
import java.io.{ObjectInputStream, DataInputStream, DataOutputStream}
import java.math.BigInteger

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version: 
 */
object TestClient {
  def main(args: Array[String]) = {

    /*
    val aliceClient = new CircuitQuery()
    aliceClient.run(3492, Array("5"))
    */

    println("To connect")
    val sock = new Socket("localhost", 3492)
    println("Connected")
    val outStream = new DataOutputStream(sock.getOutputStream)
    val inStream = new ObjectInputStream(sock.getInputStream)

    for (i <- 0 to 5) {
      println("to write data..")
      outStream.writeInt(5 + i)
      println("finish writing. now get results..")

      println(inStream.readObject().asInstanceOf[BigInteger])
      println(inStream.readObject().asInstanceOf[BigInteger])
    }
  }
}
