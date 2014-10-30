package io.garuda.common.metrics

import com.codahale.metrics.{MetricRegistry, Gauge => CHGauge}
import nl.grons.metrics.scala._

import scala.collection.mutable

/**
 * A [[MetricBuilder]] variant that remembers each [[com.codahale.metrics.Metric]] it creates and unregisters all those
 * remembered [[com.codahale.metrics.Metric]]s in its [[ManagedMetrics.close]] method.
 *
 * @param owner Class that "owns" metrics built by this builder
 * @param registry The [[MetricRegistry]] to build and register metrics
 * @throws IllegalArgumentException If `owner` or `registry` is `null`
 */
@throws[IllegalArgumentException]("If owner or registry is null")
class ManagedMetrics(owner: Class[_], registry: MetricRegistry) {
  require(owner != null, "Argument 'owner' must not be null")
  require(registry != null, "Argument 'registry' must not be null")

  private[this] val registeredMetricNames: mutable.Set[String] = new mutable.HashSet[String]()
    with scala.collection.mutable.SynchronizedSet[String]

  /**
   * Registers a new gauge metric.
   *
   * @param name  the name of the gauge
   * @param scope the scope of the gauge or null for no scope
   */
  def gauge[A](name: String, scope: String = null)(f: => A): Gauge[A] = {
    val metName: String = metricName(name, scope)
    this.registeredMetricNames += metName
    new Gauge[A](registry.register(metName, new CHGauge[A] {
      def getValue: A = f
    }))
  }

  /**
   * Creates a new counter metric.
   *
   * @param name  the name of the counter
   * @param scope the scope of the counter or null for no scope
   */
  def counter(name: String, scope: String = null): Counter = {
    val metName: String = metricName(name, scope)
    this.registeredMetricNames += metName
    new Counter(registry.counter(metName))
  }

  /**
   * Creates a new histogram metrics.
   *
   * @param name   the name of the histogram
   * @param scope  the scope of the histogram or null for no scope
   */
  def histogram(name: String, scope: String = null): Histogram = {
    val metName: String = metricName(name, scope)
    this.registeredMetricNames += metName
    new Histogram(registry.histogram(metName))
  }

  /**
   * Creates a new meter metric.
   *
   * @param name the name of the meter
   * @param scope the scope of the meter or null for no scope
   */
  def meter(name: String, scope: String = null): Meter = {
    val metName: String = metricName(name, scope)
    this.registeredMetricNames += metName
    new Meter(registry.meter(metName))
  }

  /**
   * Creates a new timer metric.
   *
   * @param name the name of the timer
   * @param scope the scope of the timer or null for no scope
   */
  def timer(name: String, scope: String = null): Timer = {
    val metName: String = metricName(name, scope)
    this.registeredMetricNames += metName
    new Timer(registry.timer(metName))
  }

  private[this] def metricName(name: String, scope: String = null): String =
    MetricBuilder.metricName(owner, Seq(name, scope))

  /**
   * Close this [[ManagedMetrics]], taking care to unregister all [[com.codahale.metrics.Metric]]s it has created
   * from the underlying [[MetricRegistry]].
   */
  def close(): Unit = {
    this.registeredMetricNames.foreach { metName => registry.remove(metName)}
  }
}
