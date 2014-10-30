package io.garuda.server

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec._
import io.garuda.common.channel.ChannelClosingUncaughtExceptionInboundHandler
import io.garuda.common.logging.slf4j._
import io.garuda.common.metrics.{GlobalPduTrafficMetricsDuplexHandler, GlobalTrafficMetricsDuplexHandler, PerChannelPduTrafficMetricsDuplexHandler, PerChannelTrafficMetricsDuplexHandler}
import io.garuda.server.session.ServerSessionChannelHandler
import io.netty.channel.group.ChannelGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.handler.logging.LoggingHandler

/**
 * Created by obergner on 27.03.14.
 */
protected[server] class ServerChannelInitializer(config: ServerConfig, channelGroup: ChannelGroup)
  extends ChannelInitializer[SocketChannel]
  with Logging {

  private[this] val loggingHandler = new LoggingHandler

  private[this] val globalTrafficMetricsDuplexHandler =
    GlobalTrafficMetricsDuplexHandler(config.metricRegistry, config.workerEventLoopGroup.next())

  private[this] val globalPduTrafficMetricsDuplexHandler = GlobalPduTrafficMetricsDuplexHandler(config.metricRegistry)

  private[this] val smppPduFrameDecoder = new SmppPduFrameDecoder(new SmppPduDecoder)
  // For starters, we only want to receive one bind request
  smppPduFrameDecoder.setSingleDecode(true)

  private[this] val smppPduOneToOneEncoder = new SmppPduOneToOneEncoder(new SmppPduEncoder)

  def initChannel(ch: SocketChannel): Unit = {
    ch.pipeline
      .addLast(ClearMdcInboundHandler.Name, ClearMdcInboundHandler)
      .addLast(ChannelRegistration.Name, ChannelRegistration)
      .addLast(GlobalTrafficMetricsDuplexHandler.Name, globalTrafficMetricsDuplexHandler)
      .addLast(PerChannelTrafficMetricsDuplexHandler.Name, PerChannelTrafficMetricsDuplexHandler(config.metricRegistry))
      .addLast(StoreSessionContextInMdcInboundHandler.Name, StoreSessionContextInMdcInboundHandler)
      .addLast("incoming/outgoing-pdu-logger", loggingHandler)
      .addLast(SmppPduFrameDecoder.Name, smppPduFrameDecoder)
      .addLast(SmppPduOneToOneEncoder.Name, smppPduOneToOneEncoder)
      .addLast(GlobalPduTrafficMetricsDuplexHandler.Name, globalPduTrafficMetricsDuplexHandler)
      .addLast(PerChannelPduTrafficMetricsDuplexHandler.Name, PerChannelPduTrafficMetricsDuplexHandler(config.metricRegistry, ch))
      .addLast(ServerSessionChannelHandler.Name, ServerSessionChannelHandler(config))
      .addLast(ChannelClosingUncaughtExceptionInboundHandler.Name, ChannelClosingUncaughtExceptionInboundHandler)
      .addLast(StoreSessionContextInMdcOutboundHandler.Name, StoreSessionContextInMdcOutboundHandler)
    logger.info(s"CHANNEL INITIALIZED: initial channel pipeline for channel ${ch} -> ${ch.pipeline()}")
  }

  private[this] object ChannelRegistration extends ChannelInboundHandlerAdapter {

    val Name = "channel-registration"

    override def channelActive(ctx: ChannelHandlerContext): Unit = {
      channelGroup.add(ctx.channel())
      ctx.fireChannelActive()
    }
  }

}
