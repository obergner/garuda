package io.garuda.common.logging.slf4j

import io.garuda.common.authentication._
import io.garuda.common.logging._
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelOutboundHandlerAdapter, ChannelPromise}
import org.slf4j.MDC

/**
 * A [[ChannelOutboundHandlerAdapter]] that registers the ID of the current [[io.garuda.common.authentication.System]]
 * and the current [[io.netty.channel.Channel]] in SLF4J's [[MDC]]. Upon request completion both datums will be removed.
 *
 * User: obergner
 * Date: 20.10.13
 * Time: 18:02
 */
@Sharable
object StoreSessionContextInMdcOutboundHandler extends ChannelOutboundHandlerAdapter {

  val Name = "store-session-context-in-logging-thread-context-outbound-handler"

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    try {
      MDC.put(ChannelContextKey, ctx.channel().toString)
      val remoteSystem = ctx.channel().attr(RemoteSystemAttributeKey)
      if (remoteSystem.get() != null) {
        MDC.put(RemoteSystemIdContextKey, remoteSystem.get().id)
      }
      ctx.write(msg, promise)
    } finally {
      MDC.remove(RemoteSystemIdContextKey)
      MDC.remove(ChannelContextKey)
    }
  }
}
