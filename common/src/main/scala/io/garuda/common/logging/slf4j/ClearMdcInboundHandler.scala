package io.garuda.common.logging.slf4j

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import org.slf4j.MDC

/**
 * A [[ChannelInboundHandlerAdapter]] that clears the current SLF4J [[MDC]]. Should be the ''first'' inbound handler.
 */
@Sharable
object ClearMdcInboundHandler extends ChannelInboundHandlerAdapter {

  val Name = "clear-logging-thread-context-inbound-handler"

  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    try {
      MDC.clear()
      ctx.fireChannelRead(msg)
    } finally {
      MDC.clear()
    }
  }
}
