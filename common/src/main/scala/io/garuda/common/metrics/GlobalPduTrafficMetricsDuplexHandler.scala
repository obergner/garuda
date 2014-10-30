package io.garuda.common.metrics

import com.codahale.metrics.MetricRegistry
import io.garuda.codec.pdu.Pdu
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelDuplexHandler, ChannelHandlerContext, ChannelPromise}
import nl.grons.metrics.scala.Meter

/**
 * Created by obergner on 06.04.14.
 */
@Sharable
class GlobalPduTrafficMetricsDuplexHandler(protected[this] val metricRegistry: MetricRegistry)
  extends ChannelDuplexHandler
  with ManagedMetricsSupport {

  import io.garuda.common.metrics.GlobalPduTrafficMetricsDuplexHandler._

  val globalInboundPduRate: Meter = metrics.meter(GlobalInboundPduRate)

  val globalOutboundPduRate: Meter = metrics.meter(GlobalOutboundPduRate)

  override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
    try {
      unregisterAllMetrics()
    } finally {
      super.handlerRemoved(ctx)
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    assert(msg.isInstanceOf[Pdu],
      s"This handler operates on messages of type ${classOf[Pdu].getName}, yet I got ${msg.getClass.getName}")
    try {
      globalInboundPduRate.mark()
    } finally {
      super.channelRead(ctx, msg)
    }
  }

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    assert(msg.isInstanceOf[Pdu],
      s"This handler operates on messages of type ${classOf[Pdu].getName}, yet I got ${msg.getClass.getName}")
    try {
      globalOutboundPduRate.mark()
    } finally {
      super.write(ctx, msg, promise)
    }
  }
}

object GlobalPduTrafficMetricsDuplexHandler {

  val Name = "global-pdu-traffic-metrics-handler"

  val GlobalInboundPduRate = "global-inbound-pdu-rate"

  val GlobalOutboundPduRate = "global-outbound-pdu-rate"

  def apply(metricRegistry: MetricRegistry): GlobalPduTrafficMetricsDuplexHandler =
    new GlobalPduTrafficMetricsDuplexHandler(metricRegistry)
}
