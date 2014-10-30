package io.garuda.common.metrics

import com.codahale.metrics.MetricRegistry
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.traffic.ChannelTrafficShapingHandler

/**
 * Created by obergner on 05.04.14.
 */
class PerChannelTrafficMetricsDuplexHandler(protected[this] val metricRegistry: MetricRegistry)
  extends ChannelTrafficShapingHandler(1000L)
  with ManagedMetricsSupport {

  import io.garuda.common.metrics.PerChannelTrafficMetricsDuplexHandler._

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    try {
      registerTrafficGauges(ctx)
    } finally {
      super.handlerAdded(ctx)
    }
  }

  private[this] def registerTrafficGauges(ctx: ChannelHandlerContext): Unit = {
    metrics.gauge[String](ChannelId, ctx.channel().toString) {
      ctx.channel().toString
    }
    metrics.gauge[Long](PerChannelReadBytes1Second, ctx.channel().toString) {
      trafficCounter().lastReadBytes()
    }
    metrics.gauge[Long](PerChannelWrittenBytes1Second, ctx.channel().toString) {
      trafficCounter().lastWrittenBytes()
    }
    metrics.gauge[Long](PerChannelCumulativeReadBytes, ctx.channel().toString) {
      trafficCounter().cumulativeReadBytes()
    }
    metrics.gauge[Long](PerChannelCumulativeWrittenBytes, ctx.channel().toString) {
      trafficCounter().cumulativeWrittenBytes()
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

object PerChannelTrafficMetricsDuplexHandler {

  val Name = "per-channel-traffic-metrics-handler"

  val ChannelId = "channel-id"

  val PerChannelReadBytes1Second = "per-channel-read-bytes-1s"

  val PerChannelWrittenBytes1Second = "per-channel--written-bytes-1s"

  val PerChannelCumulativeReadBytes = "per-channel-cumulative-read-bytes"

  val PerChannelCumulativeWrittenBytes = "per-channel-cumulative-written-bytes"

  def apply(metricRegistry: MetricRegistry): PerChannelTrafficMetricsDuplexHandler =
    new PerChannelTrafficMetricsDuplexHandler(metricRegistry)
}
