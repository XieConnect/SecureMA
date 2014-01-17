package fastgc

import java.net.Socket
import java.io.DataOutputStream

/**
  * Refer to README for details.
  * Author: Wei Xie
  * Version:
  */
object TestServer {
   def main(args: Array[String]) = {
//     val aliceClient = new CircuitQuery()
//     aliceClient.run(3491, Array("1"))

     println("To connect")
     val sock = new Socket("localhost", 3491)
     println("Connected")
     val outStream = new DataOutputStream(sock.getOutputStream)
     println("to write data..")
     outStream.writeBytes("1")
     println("finish")
   }
 }
