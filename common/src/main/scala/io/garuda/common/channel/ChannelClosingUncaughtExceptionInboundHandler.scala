package io.garuda.common.channel

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.ErrorCode
import io.garuda.codec.pdu.{GenericNack, Unbind}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}

/**
 * A [[ChannelInboundHandlerAdapter]] that upon receiving an uncaught exception will
 * - send out a [[GenericNack]],
 * - send out an [[Unbind]], and
 * - close the current [[io.netty.channel.Channel]]
 *
 * This handler should be the '''last''' inbound handler in a pipeline.
 */
@Sharable
object ChannelClosingUncaughtExceptionInboundHandler
  extends ChannelInboundHandlerAdapter
  with Logging {

  val Name: String = "channel-closing-uncaught-exception-inbound-handler"

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error(s"An uncaught exception was thrown on channel ${ctx.channel} - WILL SEND GenericNack, Unbind AND CLOSE THIS CHANNEL: ${cause.getMessage}", cause)
    ctx.write(GenericNack(ErrorCode.ESME_RSYSERR.code, 0, cause.getMessage))
    ctx.writeAndFlush(Unbind(1)).addListener(ChannelFutureListener.CLOSE)
  }
}
