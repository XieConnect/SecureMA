package edu.vanderbilt.hiplab.metaanalysis

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffer
import java.nio.charset.Charset
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.math.BigInteger

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version: 
 */
object MediatorService {
  class ServerHandler extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      try {
        val buffer = e.getMessage.asInstanceOf[ChannelBuffer].toString(Charset.forName("UTF-8")).trim
        println("Received: " + buffer)

        if (buffer.nonEmpty) {
          val input = buffer.split(" +")
          val args = Helpers.prepareInputs(new BigInteger(input(0)))

          val result = Mediator.main( args :+ input(1) )
          println("Result: " + result)
        }

      } catch { case e: Exception =>
        e.printStackTrace()
      }


      println("Finished processing.")
      //e.getChannel.write(e.getMessage)
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
        def getPipeline: ChannelPipeline = Channels.pipeline(new ServerHandler())
      })

      bootstrap.bind(new InetSocketAddress(port))
    }
  }


  // 3490 port
  def main(args: Array[String]) = {
    if (args.length < 1) {
      println("ERROR: need to provide PORT number for ManagerService.")
    } else {
      val port = try { args(0).toInt } catch { case _: Exception => 3406 }
      new ServiceServer(port).run()
    }
  }
}
