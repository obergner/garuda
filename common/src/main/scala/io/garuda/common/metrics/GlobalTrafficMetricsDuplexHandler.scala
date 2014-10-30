package io.garuda.common.metrics

import com.codahale.metrics.MetricRegistry
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import io.netty.util.concurrent.EventExecutor

/**
 * Created by obergner on 05.04.14.
 */
@Sharable
class GlobalTrafficMetricsDuplexHandler(protected[this] val metricRegistry: MetricRegistry, executor: EventExecutor)
  extends GlobalTrafficShapingHandler(executor)
  with ManagedMetricsSupport {

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    try {
      registerTrafficGauges()
    } finally {
      super.handlerAdded(ctx)
    }
  }

  private[this] def registerTrafficGauges(): Unit = {
    metrics.gauge[Long](GlobalTrafficMetricsDuplexHandler.OverallReadBytes1Second) {
      trafficCounter().lastReadBytes()
    }
    metrics.gauge[Long](GlobalTrafficMetricsDuplexHandler.OverallWrittenBytes1Second) {
      trafficCounter().lastWrittenBytes()
    }
    metrics.gauge[Long](GlobalTrafficMetricsDuplexHandler.OverallCumulativeReadBytes) {
      trafficCounter().cumulativeReadBytes()
    }
    metrics.gauge[Long](GlobalTrafficMetricsDuplexHandler.OverallCumulativeWrittenBytes) {
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

object GlobalTrafficMetricsDuplexHandler {

  val Name = "global-traffic-metrics-handler"

  val OverallReadBytes1Second = "global-read-bytes-1s"

  val OverallWrittenBytes1Second = "global--written-bytes-1s"

  val OverallCumulativeReadBytes = "global-cumulative-read-bytes"

  val OverallCumulativeWrittenBytes = "global-cumulative-written-bytes"

  def apply(metricRegistry: MetricRegistry, executor: EventExecutor): GlobalTrafficMetricsDuplexHandler =
    new GlobalTrafficMetricsDuplexHandler(metricRegistry, executor)
}
