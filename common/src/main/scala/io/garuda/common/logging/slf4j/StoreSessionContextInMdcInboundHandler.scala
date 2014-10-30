package io.garuda.common.logging.slf4j

import io.garuda.common.authentication.RemoteSystemAttributeKey
import io.garuda.common.logging._
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import org.slf4j.MDC


/**
 * A [[ChannelInboundHandlerAdapter]] that registers the ID of the current [[io.garuda.common.authentication.System]]
 * and the current [[io.netty.channel.Channel]] in SLF4J's [[MDC]]. Upon request completion, both datums will be removed.
 *
 * User: obergner
 * Date: 20.10.13
 * Time: 03:41
 */
@Sharable
object StoreSessionContextInMdcInboundHandler extends ChannelInboundHandlerAdapter {

  val Name = "store-session-context-in-logging-thread-context-inbound-handler"

  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    try {
      MDC.put(ChannelContextKey, ctx.channel().toString)
      val remoteSystem = ctx.channel().attr(RemoteSystemAttributeKey)
      if (remoteSystem.get() != null) {
        MDC.put(RemoteSystemIdContextKey, remoteSystem.get().id)
      }
      ctx.fireChannelRead(msg)
    } finally {
      MDC.remove(RemoteSystemIdContextKey)
      MDC.remove(ChannelContextKey)
    }
  }
}
