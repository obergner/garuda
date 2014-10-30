package io.garuda.common.builder

import com.codahale.metrics.{MetricFilter, MetricRegistry}
import io.garuda.common.resources.ResourcesRegistry

import scala.concurrent.duration.Duration

/**
 * A context object storing a [[MetricRegistry]] and a [[ResourcesRegistry]], two components that are commonly needed
 * when building other components. Meant to be passed down the call chain.
 *
 * @param metricRegistry The [[MetricRegistry]] to use
 * @param resourcesRegistry The [[ResourcesRegistry]] to use
 * @throws IllegalArgumentException If one of the parameters is `null`
 *
 * @see [[MetricRegistry]]<br/>
 *      [[ResourcesRegistry]]
 */
@throws[IllegalArgumentException]("If one of the parameters is null")
case class BuilderContext(metricRegistry: MetricRegistry, resourcesRegistry: ResourcesRegistry) {
  require(metricRegistry != null, "Argument 'metricRegistry' must not be null")
  require(resourcesRegistry != null, "Argument 'resourcesRegistry' must not be null")

  /**
   * Open this [[BuilderContext]], i.e. start monitoring resources for expiration.
   */
  def open(): Unit = this.resourcesRegistry.start()

  /**
   * Close this [[BuilderContext]], i.e.
   *
   * - clear all [[com.codahale.metrics.Metric]]s from its [[MetricRegistry]] and
   * - stop its [[ResourcesRegistry]]
   */
  def close(): Unit = {
    this.metricRegistry.removeMatching(MetricFilter.ALL)
    this.resourcesRegistry.stop()
  }
}

/**
 * Companion object for [[BuilderContext]].
 */
object BuilderContext {

  /**
   * Create a new [[BuilderContext]] using a default [[MetricRegistry]] instance with no special configuration.
   *
   * @param name The `name` of the [[ResourcesRegistry]] in the returned [[BuilderContext]]
   * @param shutdownTimeout The `shutdownTimeout` of the [[ResourcesRegistry]] in the returned [[BuilderContext]]
   * @return A new [[BuilderContext]] using a default [[MetricRegistry]]
   */
  def withDefaultMetricRegistry(name: String, shutdownTimeout: Duration) = {
    val metricReg = new MetricRegistry
    val resourcesReg = new ResourcesRegistry(name, shutdownTimeout, metricReg)
    BuilderContext(metricReg, resourcesReg)
  }
}
