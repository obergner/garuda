package io.garuda.server.session

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu._
import io.garuda.common.spi.session.{Session, SessionChannelAdapter}
import io.garuda.server.ServerConfig
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 27.10.13
 * Time: 10:15
 * To change this template use File | Settings | File Templates.
 */
class ServerSessionChannelHandler(serverConfig: ServerConfig)
  extends ChannelInboundHandlerAdapter with Logging {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info(s"ACTIVE:    channel ${ctx.channel} has been activated - will initiate session using ${serverConfig.commonSessionConfig}")
    val unboundSmppServerSession = serverConfig.serverSessionFactory(ctx.channel())
    assert(ctx.channel().attr(Session.AttachedSmppServerSession).compareAndSet(null, unboundSmppServerSession),
      s"Expected no Session to be associated with channel ${ctx.channel}")
    unboundSmppServerSession.channelAdapter.channelActivated(ctx)
    logger.info(s"ACTIVATED: ${unboundSmppServerSession} on channel ${ctx.channel}")

    ctx.fireChannelActive()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    logger.info(s"RECEIVED: ${msg} ...")
    msg match {
      case bind: BaseBind[_] => mandatoryServerSessionAdapter(ctx).bindRequestReceived(ctx, bind)
      case unbind: Unbind => mandatoryServerSessionAdapter(ctx).unbindRequestReceived(ctx, unbind)
      case enquireLinkRequest: EnquireLink => mandatoryServerSessionAdapter(ctx).enquireLinkRequestReceived(ctx, enquireLinkRequest)
      case submitSmRequest: SubmitSm => mandatoryServerSessionAdapter(ctx).pduRequestReceived[SubmitSmResponse, SubmitSm](ctx, submitSmRequest)
      case _ => throw new IllegalArgumentException(s"Received unsupported PDU: ${msg}")
    }
    logger.info(s"RECEIVED: ${msg} (DONE)")
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    logger.info(s"INACTIVE: ${ctx.channel} has been deactivated (closed) - will stop associated session (if any)")
    serverSessionAdapter(ctx).foreach(adapter => adapter.channelClosed(ctx))
    logger.info(s"INACTIVE: Session associated with channel ${ctx.channel} has been closed")
  }


  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error(s"CAUGHT: exception on channel ${ctx.channel} - associated session (if any) will be closed: ${cause.getMessage}", cause)
    serverSessionAdapter(ctx).foreach(adapter => adapter.channelClosed(ctx))
    ctx.fireExceptionCaught(cause)
  }

  private[this] def serverSessionAdapter(ctx: ChannelHandlerContext): Option[SessionChannelAdapter] = {
    val serverSession = ctx.channel().attr(Session.AttachedSmppServerSession).get()
    if (serverSession != null) Some(serverSession.channelAdapter) else None
  }

  private[this] def mandatoryServerSessionAdapter(ctx: ChannelHandlerContext): SessionChannelAdapter = {
    serverSessionAdapter(ctx).getOrElse(throw new
        IllegalStateException(s"No session is attached to channel ${ctx.channel}"))
  }
}

object ServerSessionChannelHandler {

  val Name = "smpp-server-session-handler"

  def apply(serverConfig: ServerConfig): ServerSessionChannelHandler =
    new ServerSessionChannelHandler(serverConfig)
}

