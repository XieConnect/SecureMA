package edu.vanderbilt.hiplab.securema

import org.jboss.netty.channel._
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.math.BigInteger
import org.jboss.netty.handler.codec.serialization.{ClassResolvers, ObjectDecoder, ObjectEncoder}
import Program.EstimateNServer

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version:
 */
object ServerService {
  var server: EstimateNServer = _

  class ServerHandler extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      try {
        val args = e.getMessage.asInstanceOf[Array[String]]
        println("Got: " + args(0))

        server.setInputs(new BigInteger("1"))
        server.runOnline()

        /*
        for (result <- server.results) {
          println("Result: " + result)
        }
        */

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


  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("ERROR: need to provide PORT number for Service.")
    } else {
      //val port = try { args(0).toInt } catch { case _: Exception => 3492 }
      val port = 3492

      server = new EstimateNServer(80, 80)
      server.runOffline()

      println("Now accepting new requests...")
      new ServiceServer(port).run()
    }
  }
}
