package io.garuda.common.metrics

import com.codahale.metrics.MetricRegistry

/**
 * Created by obergner on 19.09.14.
 */
trait ManagedMetricsSupport {

  private[this] lazy val metricBuilder = new ManagedMetrics(getClass, metricRegistry)

  /**
   * The MetricBuilder that can be used for creating timers, counters, etc.
   */
  protected[this] def metrics: ManagedMetrics = metricBuilder

  /**
   * The MetricRegistry where created metrics are registered.
   */
  protected[this] val metricRegistry: MetricRegistry

  /**
   * Close [[ManagedMetrics]], taking care to unregister all [[com.codahale.metrics.Metric]]s from the
   * underlying [[MetricRegistry]].
   *
   * @see [[ManagedMetrics.close]]
   */
  protected[this] def unregisterAllMetrics(): Unit = metrics.close()
}
