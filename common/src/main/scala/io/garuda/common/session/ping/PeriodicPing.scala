package io.garuda.common.session.ping

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.{EnquireLink, EnquireLinkResponse}
import io.garuda.common.logging.slf4j.StoreChannelInMdcChannelFutureListener
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.{Timeout, Timer, TimerTask}

/**
 * Created by obergner on 30.03.14.
 */
final class PeriodicPing(periodicPingConfig: PeriodicPingConfig) extends ChannelInboundHandlerAdapter with Logging {

  private[this] val pingTimeoutHandle: AtomicReference[Timeout] = new AtomicReference[Timeout]()

  private[this] val nextPeriodicPingHandle: AtomicReference[Timeout] = new AtomicReference[Timeout]()

  private[this] val pingTimer: Timer = periodicPingConfig.periodicPingTimer()


  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    start(ctx.channel())
    super.handlerAdded(ctx)
  }

  private[this] def start(channel: Channel): Unit = {
    logger.info(s"Will start to periodically send EnquireLink (ping) on channel ${channel}} in [${periodicPingConfig.pingIntervalMillis}] ms")
    channel.closeFuture().addListener(StoreChannelInMdcChannelFutureListener(future => {
      stop(channel)
    }))
    scheduleNextPeriodicPing(channel)
  }

  private[this] def stop(channel: Channel): Unit = {
    val pingTimeoutHdl = pingTimeoutHandle.get
    if (pingTimeoutHdl != null && !pingTimeoutHdl.isCancelled) {
      pingTimeoutHdl.cancel()
      logger.debug(s"Canceled enquire link response timeout")
    }
    val nextPeriodicPingHdl = nextPeriodicPingHandle.get
    if (nextPeriodicPingHdl != null && !nextPeriodicPingHdl.isCancelled) {
      nextPeriodicPingHdl.cancel()
      logger.debug(s"Canceled next periodic ping timeout")
    }
    logger.info(s"Periodic ping on channel ${channel} has been stopped")
  }

  private[this] def scheduleNextPeriodicPing(channel: Channel): Unit = {
    val timeout = pingTimer.newTimeout(new
        SendPeriodicPing(channel), periodicPingConfig.pingIntervalMillis, TimeUnit.MILLISECONDS)
    nextPeriodicPingHandle.set(timeout)
  }


  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case enquireLinkResp: EnquireLinkResponse => enquireLinkResponseReceived(ctx, enquireLinkResp)
    }
    super.channelRead(ctx, msg)
  }

  private[this] def enquireLinkResponseReceived(ctx: ChannelHandlerContext, enquireLinkResp: EnquireLinkResponse): Unit = {
    logger.debug(s"Received ${enquireLinkResp} (pong) on channel ${ctx.channel}")
    val pingTimeoutHdl = pingTimeoutHandle.get
    if (pingTimeoutHdl != null) {
      /*
       * It could only be == null if we receive (however unlikely that is) an EnquireLinkResp before we even sent our
       * first EnquireLink. What the heck: let's be paranoid.
       */
      pingTimeoutHdl.cancel()
    }
    scheduleNextPeriodicPing(ctx.channel())
  }

  private[this] def scheduleCloseSessionOnPingTimeout(channel: Channel): Unit = {
    val timeout = pingTimer.newTimeout(new
        CloseSessionOnPingTimeout(channel), periodicPingConfig.pingResponseTimeoutMillis, TimeUnit.MILLISECONDS)
    pingTimeoutHandle.set(timeout)
  }

  private[this] final class SendPeriodicPing(channel: Channel) extends TimerTask {

    val NextSeqNo = new AtomicInteger(1)

    override def run(timeout: Timeout): Unit = {
      if (timeout.isCancelled) {
        logger.info(s"${getClass.getSimpleName} has been canceled - will not send EnquireLink (ping)")
        return
      }
      logger.debug(s"Sending EnquireLink ...")
      val ping: EnquireLink = EnquireLink(NextSeqNo.getAndIncrement)
      channel.writeAndFlush(ping).addListener(StoreChannelInMdcChannelFutureListener(future => {
        if (future.isSuccess) {
          logger.debug(s"Successfully sent EnquireLink (ping)")
        } else if (future.cause != null) {
          logger.error(s"Failed to send EnquireLink (ping) - THIS SESSION WILL BE CLOSED: ${future.cause.getMessage}", future.cause)
          channel.close().sync()
        }
      }))

      scheduleCloseSessionOnPingTimeout(channel)
    }
  }

  private[this] final class CloseSessionOnPingTimeout(channel: Channel) extends TimerTask {

    override def run(timeout: Timeout): Unit = {
      if (timeout.isCancelled) {
        logger.debug(s"${getClass.getSimpleName} has been canceled (very likely because EnquireLinkResp (pong) has been received in time)")
        return
      }
      logger.warn(s"Did not receive an EnquireLinkResp (pong) in response to a previously sent EnquireLink (ping) " +
        s"within timeout of [${periodicPingConfig.pingResponseTimeoutMillis}] ms - THIS SESSION WILL BE CLOSED")
      channel.close().sync()
    }
  }

}

object PeriodicPing {

  val Name = "periodic-ping"

  def apply(periodicPingConfig: PeriodicPingConfig): PeriodicPing =
    new PeriodicPing(periodicPingConfig)
}
