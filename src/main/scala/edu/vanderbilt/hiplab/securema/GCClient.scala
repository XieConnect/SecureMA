package edu.vanderbilt.hiplab.securema

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.serialization.{ClassResolvers, ObjectDecoder, ObjectEncoder}
import java.net.InetSocketAddress
import java.math.BigInteger

/**
 * Garbled Circuit client (for evaluating ln(x))
 * Author: Wei Xie
 * Version:
 */
class GCClient {
  // final result from Fairplay
  // For Bob: result = {alpha, beta}
  // For Alice: result = {encryptions of power of alpha, beta}
  var result: Array[BigInteger] = _

  /**
   *
   * @param args of the form: {input_values, Fairplay_socket_port}
   */
  class ClientHandler(args: Array[String]) extends SimpleChannelUpstreamHandler {
    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      e.getChannel.write(args)
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      result = e.getMessage.asInstanceOf[Array[BigInteger]]
      e.getChannel.close()
    }
  }

  /**
   * Main entry to query and obtain Fairplay results
   * @param port port of Fairplay server you want to connect to
   * @param args
    */
  def run(port: Int, args: Array[String]) = {
    val bootstrap = new ClientBootstrap(
      new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()
      )
    )

    bootstrap.setPipelineFactory( new ChannelPipelineFactory {
      override def getPipeline: ChannelPipeline = Channels.pipeline(
        new ObjectEncoder(),
        new ObjectDecoder(ClassResolvers.cacheDisabled(getClass.getClassLoader)),
        new ClientHandler(args)
      )
    })

    try {
      val future = bootstrap.connect(new InetSocketAddress("localhost", port))
      future.getChannel.getCloseFuture.awaitUninterruptibly()
      bootstrap.releaseExternalResources()
      bootstrap.shutdown()

    } catch { case e: Exception =>
      e.printStackTrace()
    }

  }
}