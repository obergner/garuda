package io.garuda.common.session.ping

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import io.garuda.common.builder.{BuilderContext, NodeBuilder}
import io.netty.util.{HashedWheelTimer, Timer}

/**
 * Configures a session's periodic ping behaviour.
 *
 * @param pingIntervalMillis Interval in milliseconds for sending out ''EnquireLink'' ("ping") requests. Must be > 0.
 * @param pingResponseTimeoutMillis Timeout in milliseconds to wait for an ''EnquireLinkResp'' ("pong") before closing
 *                                  the current session. Must be > 0.
 * @param periodicPingTimerFactory A factory for [[Timer]] instances needed for pinging a remote client at regular
 *                                 intervals. If `null` a default factory will be used.
 * @param builderContext The [[BuilderContext]] to use. Must not be `null`.
 * @throws IllegalArgumentException If one of the required arguments is `null` or otherwise illegal
 */
@throws[IllegalArgumentException]("If one of the required arguments is null or otherwise illegal")
class PeriodicPingConfig(val pingIntervalMillis: Long,
                         val pingResponseTimeoutMillis: Long,
                         periodicPingTimerFactory: (PeriodicPingConfig => Timer),
                         builderContext: BuilderContext) {
  require(pingIntervalMillis > 0, "Argument 'pingIntervalMillis' needs to be > 0")
  require(pingResponseTimeoutMillis > 0, "Argument 'pingResponseTimeoutMillis' needs to be > 0")
  require(builderContext != null, "Argument 'builderContext' must not be null")

  /**
   * Default factory for [[Timer]] instances needed for pinging a remote client at regular intervals. This factory will
   * always return the same [[Timer]] instance.
   */
  val DefaultPeriodicPingTimerFactory: (PeriodicPingConfig => Timer) = Function.const(new
      HashedWheelTimer(new ThreadFactory {

        val nextThreadId: AtomicInteger = new AtomicInteger(1)

        override def newThread(r: Runnable): Thread = new
            Thread(r, "periodic-ping-timer#" + nextThreadId.getAndIncrement)
      }))

  private[this] val periodicPingTimerFac: (PeriodicPingConfig => Timer) =
    if (periodicPingTimerFactory != null) periodicPingTimerFactory else DefaultPeriodicPingTimerFactory

  private[this] val registeringPeriodicPingTimerFactory: PeriodicPingConfig => Timer =
    builderContext.resourcesRegistry.registerTimer(periodicPingTimerFac)

  /**
   * Create a new or cached [[Timer]] instance, using the configured `periodicPingTimerFactory`.
   *
   * '''Note''' that the [[Timer]] returned by this method will be registered with the configured [[BuilderContext]].
   *
   * @return A new or cached [[Timer]] instance
   */
  def periodicPingTimer(): Timer = registeringPeriodicPingTimerFactory(this)
}

/**
 * Companion object for [[PeriodicPingConfig]], containing default values for most of its parameters.
 */
object PeriodicPingConfig {

  /**
   * The default [[PeriodicPingConfig.pingIntervalMillis]] (5000 ms).
   */
  val DefaultPingIntervalMillis: Long = 5000L

  /**
   * The default [[PeriodicPingConfig.pingResponseTimeoutMillis]] (10.000 ms).
   */
  val DefaultPingResponseTimeoutMillis: Long = 10000L

  /**
   * Return a default [[PeriodicPingConfig]], using the supplied [[BuilderContext]].
   *
   * @param builderContext The [[BuilderContext]] injected into the returned [[PeriodicPingConfig]]
   * @return A new [[PeriodicPingConfig]] instances with most of its fields set to their default values
   * @throws IllegalArgumentException If `builderContext` is `null`
   */
  @throws[IllegalArgumentException]("If builderContext is null")
  def default(builderContext: BuilderContext): PeriodicPingConfig = new PeriodicPingConfig(DefaultPingIntervalMillis,
    DefaultPingResponseTimeoutMillis,
    null,
    builderContext)

  /**
   * Create a new [[PeriodicPingConfig]] instance from the supplied parameters.
   *
   * @param pingIntervalMillis Interval in milliseconds for sending out ''EnquireLink'' ("ping") requests. Must be > 0.
   * @param pingResponseTimeoutMillis Timeout in milliseconds to wait for an ''EnquireLinkResp'' ("pong") before closing
   *                                  the current session. Must be > 0.
   * @param periodicPingTimerFactory A factory for [[Timer]] instances needed for pinging a remote client at regular
   *                                 intervals. If `null` a default factory will be used.
   * @param builderContext The [[BuilderContext]] to use. Must not be `null`.
   * @throws IllegalArgumentException If one of the parameters except for 'periodicPingTimerFactory' is `null` or otherwise illegal
   * @return A new [[PeriodicPingConfig]] instance built from the supplied parameters
   */
  @throws[IllegalArgumentException]("If one of the parameters except for 'periodicPingTimerFactory' is null or otherwise illegal")
  def apply(pingIntervalMillis: Long,
            pingResponseTimeoutMillis: Long,
            periodicPingTimerFactory: (PeriodicPingConfig => Timer),
            builderContext: BuilderContext): PeriodicPingConfig =
    new PeriodicPingConfig(pingIntervalMillis, pingResponseTimeoutMillis, periodicPingTimerFactory, builderContext)
}

/**
 * Base trait for [[PeriodicPingConfig]] builders. Supports hierarchical builders.
 *
 * @tparam T Self type, i.e. the concrete type of the class implementing this trait
 * @tparam X The type of product to build. For non-hierarchical builders this will be [[PeriodicPingConfig]]
 */
trait PeriodicPingConfigBuilderSupport[T, X] extends NodeBuilder[PeriodicPingConfig, X] {
  self: T =>

  var pingIntervalMillis: Long = PeriodicPingConfig.DefaultPingIntervalMillis

  var pingResponseTimeMillis: Long = PeriodicPingConfig.DefaultPingResponseTimeoutMillis

  var periodicPingTimerFactory: (PeriodicPingConfig => Timer) = null

  /**
   * Interval in milliseconds for sending out ''EnquireLink'' ("ping") requests. Must be > 0.
   *
   * @param interval Interval in milliseconds for sending out ''EnquireLink'' ("ping") requests. Must be > 0.
   * @throws IllegalArgumentException If `interval` <= 0
   * @return this
   */
  @throws[IllegalArgumentException]("If interval <= 0")
  def pingIntervalMillis(interval: Long): T = {
    require(interval > 0, "Ping interval (ms) must be > 0: " + interval)
    this.pingIntervalMillis = interval
    this
  }

  /**
   * Timeout in milliseconds to wait for an ''EnquireLinkResp'' ("pong") before closing the current session. Must be > 0.
   *
   * @param timeout Timeout in milliseconds to wait for an ''EnquireLinkResp'' ("pong") before closing the current session. Must be > 0.
   * @throws IllegalArgumentException If `timeout` <= 0
   * @return this
   */
  @throws[IllegalArgumentException]("If timeout <= 0")
  def pingResponseTimeoutMillis(timeout: Long): T = {
    require(timeout > 0, "PingResponse timeout (ms) must be > 0: " + timeout)
    this.pingResponseTimeMillis = timeout
    this
  }

  /**
   * A factory for [[Timer]] instances needed for pinging a remote client at regular intervals. If you want the default
   * factory to be used do not call this method, but '''never''' call this method with a `null` argument.
   *
   * @param timerFactory A factory for [[Timer]] instances needed for pinging a remote client at regular intervals
   * @throws IllegalArgumentException If `timerFactory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If timerFactory is null")
  def periodicPingTimerFactory(timerFactory: PeriodicPingConfig => Timer): T = {
    require(timerFactory != null, "Periodic ping timerFactory must not be null")
    this.periodicPingTimerFactory = timerFactory
    this
  }

  @throws[IllegalArgumentException]("If builderContext is null")
  protected[this] def buildPeriodicPingConfig(builderContext: BuilderContext): PeriodicPingConfig = {
    new PeriodicPingConfig(
      this.pingIntervalMillis,
      this.pingResponseTimeMillis,
      this.periodicPingTimerFactory,
      builderContext)
  }

  /**
   * Build and return a new [[PeriodicPingConfig]] instance with the supplied [[BuilderContext]] injected into it.
   *
   * @param builderContext The [[BuilderContext]] to inject into the returned [[PeriodicPingConfig]] instance. Must not
   *                       be `null`
   * @throws IllegalArgumentException If `builderContext` is `null`
   * @return A new [[PeriodicPingConfig]] instance with the supplied [[BuilderContext]] injected into it
   */
  @throws[IllegalArgumentException]("If builderContext is null")
  override def buildNode(builderContext: BuilderContext): PeriodicPingConfig = buildPeriodicPingConfig(builderContext)
}

/**
 * A builder for [[PeriodicPingConfig]]s.
 *
 * @param builderContext The [[BuilderContext]] to inject into the [[PeriodicPingConfig]] instance built by this builder.
 *                       Must not be null.
 * @throws IllegalArgumentException If `builderContext` is `null`
 */
@throws[IllegalArgumentException]("If builderContext is null")
final class PeriodicPingConfigBuilder(builderContext: BuilderContext)
  extends PeriodicPingConfigBuilderSupport[PeriodicPingConfigBuilder, PeriodicPingConfig] {
  require(builderContext != null, "Argument 'builderContext' must not be null")

  override def build(): PeriodicPingConfig = buildPeriodicPingConfig(builderContext)
}

/**
 * Factory for [[PeriodicPingConfigBuilder]]s.
 */
object PeriodicPingConfigBuilder {

  /**
   * Create a new [[PeriodicPingConfigBuilder]] instance, using the supplied [[BuilderContext]].
   *
   * @param builderContext The [[BuilderContext]] to be used by the returned [[PeriodicPingConfigBuilder]]
   * @throws IllegalArgumentException If `builderContext` is `null`
   * @return A new [[PeriodicPingConfigBuilder]] using the supplied [[BuilderContext]]
   */
  @throws[IllegalArgumentException]("If builderContext is null")
  def apply(builderContext: BuilderContext): PeriodicPingConfigBuilder = new PeriodicPingConfigBuilder(builderContext)
}
