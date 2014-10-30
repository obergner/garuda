package io.garuda.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.group.ChannelGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

/**
 * Created by obergner on 27.03.14.
 */
object ServerBootstrapFactory extends ((ServerConfig, ChannelGroup) => ServerBootstrap) {

  override def apply(config: ServerConfig, channelGroup: ChannelGroup): ServerBootstrap = {
    val serverBootstrap = new ServerBootstrap

    serverBootstrap.group(config.bossEventLoopGroup, config.workerEventLoopGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ServerChannelInitializer(config, channelGroup))

    // Server socket options
    config.soBacklog.foreach(backlog => serverBootstrap.option[java.lang.Integer](ChannelOption.SO_BACKLOG, backlog))
    config.soReuseAddress.foreach(reuseAddr => serverBootstrap.option[java.lang.Boolean](ChannelOption.SO_REUSEADDR, reuseAddr))
    config.soReceiveBufferSizeBytes.foreach(rcvBufSize => serverBootstrap.option[java.lang.Integer](ChannelOption.SO_RCVBUF, rcvBufSize))

    // LeafNodeProduct socket options
    config.soKeepAlive.foreach(keepAlive => serverBootstrap.childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, keepAlive))
    config.soLinger.foreach(linger => serverBootstrap.childOption[java.lang.Integer](ChannelOption.SO_LINGER, linger))
    config.soSendBufferSizeBytes.foreach(sndBufSize => serverBootstrap.childOption[java.lang.Integer](ChannelOption.SO_SNDBUF, sndBufSize))
    config.tcpNoDelay.foreach(noDelay => serverBootstrap.childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, noDelay))

    serverBootstrap
  }
}
