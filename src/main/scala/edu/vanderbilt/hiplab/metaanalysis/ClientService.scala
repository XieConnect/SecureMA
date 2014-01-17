package edu.vanderbilt.hiplab.metaanalysis

import org.jboss.netty.channel._
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import java.math.BigInteger
import java.io.File
import org.jboss.netty.handler.codec.serialization.{ClassResolvers, ObjectDecoder, ObjectEncoder}
import Program.{EstimateNClient, EstimateNServer}

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version:
 */
object ClientService {
  var client: EstimateNClient = _

  class ClientHandler extends SimpleChannelUpstreamHandler {
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      try {
        val args = e.getMessage.asInstanceOf[Array[String]]
        println("Got: " + args(0))


        client.setInputs(new BigInteger("5"))
        client.runOnline()

      } catch { case e: Exception =>
        e.printStackTrace()
      }
    }
  }


  class ServiceClient(port: Int) {
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
          new ClientHandler()
        )
      })

      bootstrap.bind(new InetSocketAddress(port))
    }
  }


  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("ERROR: need to provide PORT number for AliceService.")
    } else {
      //val port = try { args(0).toInt } catch { case _: Exception => 3491 }
      val port = 3491

      client = new EstimateNClient(80)
      client.runOffline()

      println("Now accepting new requests...")
      new ServiceClient(port).run()
    }
  }
}
