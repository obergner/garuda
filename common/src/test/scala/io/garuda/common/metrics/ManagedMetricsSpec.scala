package io.garuda.common.metrics

import com.codahale.metrics.MetricRegistry
import org.specs2.mutable.{Specification, Tags}

class ManagedMetricsSpec extends Specification with Tags {

  "ManagedMetrics as a factory for Metrics" should {

    "register all Metrics created by it in its MetricRegistry" in {
      val metricRegistry: MetricRegistry = new MetricRegistry
      val metricsCountBefore = metricRegistry.getMetrics.size()

      val objectUnderTest = new ManagedMetrics(getClass, metricRegistry)
      objectUnderTest.gauge[Int]("gauge", "scope") {
        1
      }
      objectUnderTest.counter("counter", "scope")
      objectUnderTest.histogram("histogram", "scope")
      objectUnderTest.meter("meter", "scope")
      objectUnderTest.timer("timer", "scope")

      metricRegistry.getMetrics.size() must be_==(metricsCountBefore + 5)
    } tag "unit"
  }

  "ManagedMetrics as a registry for Metrics created by it" should {

    "remove all its Metrics from the underlying MetricRegistry upon close()" in {
      val metricRegistry: MetricRegistry = new MetricRegistry
      val metricsCountBefore = metricRegistry.getMetrics.size()

      val objectUnderTest = new ManagedMetrics(getClass, metricRegistry)
      objectUnderTest.gauge[Int]("gauge", "scope") {
        1
      }
      objectUnderTest.counter("counter", "scope")
      objectUnderTest.histogram("histogram", "scope")
      objectUnderTest.meter("meter", "scope")
      objectUnderTest.timer("timer", "scope")
      objectUnderTest.close()

      metricRegistry.getMetrics.size() must be_==(metricsCountBefore)
    } tag "unit"
  }
}
