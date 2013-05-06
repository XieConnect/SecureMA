package edu.vanderbilt.hiplab.metaanalysis

import org.jboss.netty.channel._
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.math.BigInteger
import java.io.File
import org.jboss.netty.handler.codec.serialization.{ClassResolvers, ObjectDecoder, ObjectEncoder}

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version:
 */
object AliceService {
  class ServerHandler extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      try {
        val args = e.getMessage.asInstanceOf[Array[String]]

        // Is "compile" necessary for Alice? not sure...
        if ( args.length > 0 && args(0).equals("init") ||
          (! new File(Helpers.property("data_directory"), Helpers.property("private_keys")).exists()) ) {
          //generateKeys()
          SFE.BOAL.Alice.main( Array("-c", Helpers.property("fairplay_script")) )

        } else if (args.length > 1) {
          // general params, plus socket_port and input values
          val aliceArgs = Array("-r", Helpers.property("fairplay_script"), "dj2j", Helpers.property("socket_server"), args(1), args(0))
          //println("To run Alice Fairplay: " + aliceArgs.mkString("  "))
          val aliceOutputs: Array[BigInteger] = SFE.BOAL.Alice.main(aliceArgs).filter(_ != null)

          // encryptions of powers of alpha, beta
          e.getChannel.write( Manager.encryptPowers(aliceOutputs(0)) :+ Helpers.encryptBeta(aliceOutputs(1)) )
        }
        println("Finished Alice's role in Fairplay.")

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


  def main(args: Array[String]) = {
    if (args.length < 1) {
      println("ERROR: need to provide PORT number for AliceService.")
    } else {
      val port = try { args(0).toInt } catch { case _: Exception => 3491 }
      new ServiceServer(port).run()
    }
  }
}
