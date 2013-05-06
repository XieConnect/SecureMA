package edu.vanderbilt.hiplab.metaanalysis

import org.jboss.netty.channel._
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.math.BigInteger
import org.jboss.netty.handler.codec.serialization.{ClassResolvers, ObjectDecoder, ObjectEncoder}
import java.io.File

/**
 * Fairplay's Bob deployed as a backend service
 * Author: Wei Xie
 * Version: 
 */
object BobService {
  class ServerHandler extends SimpleChannelUpstreamHandler {
    // Expected string message: <input_values> <port_offset>
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      try {
        val args = e.getMessage.asInstanceOf[Array[String]]

        if ( args.length > 0 && args(0).equals("init") ||
          (! new File(Helpers.property("data_directory"), Helpers.property("private_keys")).exists()) ) {
          //generateKeys()
          SFE.BOAL.Bob.main( Array("-c", Helpers.property("fairplay_script")) )

        } else if (args.length > 1) {
          // general params, plus socket_port and input values
          val bobArgs = Array("-r", Helpers.property("fairplay_script"), "dj2j", "4", args(1), args(0))
          println("To run Bob Fairplay: " + bobArgs.mkString("  "))
          val bobOutputs: Array[BigInteger] = SFE.BOAL.Bob.main(bobArgs).filter(_ != null)

          // alpha, beta
          e.getChannel.write(bobOutputs)
        }
        println("Finished Bob's role in Fairplay.")

      } catch { case e: Exception =>
        e.printStackTrace()
      }
    }
  }

  class ServiceServer(port: Int) {
    def run() = {
      val bootstrap = new ServerBootstrap(
        new NioServerSocketChannelFactory(
          Executors.newCachedThreadPool(),
          Executors.newCachedThreadPool()
        )
      )

      bootstrap.setPipelineFactory(new ChannelPipelineFactory {
        def getPipeline: ChannelPipeline = Channels.pipeline(
          new ObjectEncoder(),
          new ObjectDecoder(ClassResolvers.cacheDisabled(getClass.getClassLoader)),
          new ServerHandler()
        )
      })

      bootstrap.bind(new InetSocketAddress(port))
    }
  }

  // 3490 port
  def main(args: Array[String]) = {
    if (args.length < 1) {
      println("ERROR: need to provide PORT number for AliceService.")
    } else {
      val port = try { args(0).toInt } catch { case _: Exception => 3496 }
      new ServiceServer(port).run()
    }
  }
}
