package fastgc

import java.net.Socket
import java.io.DataOutputStream

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
    println("to write data..")
    outStream.writeBytes("5")
    println("finish")
  }
}
