package io.garuda.common.metrics

import com.codahale.metrics.MetricRegistry
import io.garuda.codec.pdu.Pdu
import io.netty.channel.{Channel, ChannelDuplexHandler, ChannelHandlerContext, ChannelPromise}

/**
 * Created by obergner on 06.04.14.
 */
class PerChannelPduTrafficMetricsDuplexHandler(protected[this] val metricRegistry: MetricRegistry, channel: Channel)
  extends ChannelDuplexHandler
  with ManagedMetricsSupport {

  import io.garuda.common.metrics.PerChannelPduTrafficMetricsDuplexHandler._

  val perChannelInboundPduRate = metrics.meter(PerChannelInboundPduRate, channel.toString)

  val perChannelOutboundPduRate = metrics.meter(PerChannelOutboundPduRate, channel.toString)

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    assert(msg.isInstanceOf[Pdu],
      s"This handler operates on messages of type ${classOf[Pdu].getName}, yet I got ${msg.getClass.getName}")
    try {
      perChannelInboundPduRate.mark()
    } finally {
      super.channelRead(ctx, msg)
    }
  }

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    assert(msg.isInstanceOf[Pdu],
      s"This handler operates on messages of type ${classOf[Pdu].getName}, yet I got ${msg.getClass.getName}")
    try {
      perChannelOutboundPduRate.mark()
    } finally {
      super.write(ctx, msg, promise)
    }
  }

  override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
    try {
      unregisterAllMetrics()
    } finally {
      super.handlerRemoved(ctx)
    }
  }
}

object PerChannelPduTrafficMetricsDuplexHandler {

  val Name = "per-channel-pdu-traffic-metrics-handler"

  val PerChannelInboundPduRate = "per-channel-inbound-pdu-rate"

  val PerChannelOutboundPduRate = "per-channel-outbound-pdu-rate"

  def apply(metricRegistry: MetricRegistry, channel: Channel): PerChannelPduTrafficMetricsDuplexHandler =
    new PerChannelPduTrafficMetricsDuplexHandler(metricRegistry, channel)
}
