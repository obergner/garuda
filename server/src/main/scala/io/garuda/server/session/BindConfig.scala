package io.garuda.server.session

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import io.garuda.common.builder.{BuilderContext, NodeBuilder}
import io.garuda.server.authentication.support.AlwaysNackAuthenticator
import io.garuda.server.spi.authentication.Authenticator
import io.netty.util.{HashedWheelTimer, Timer}

/**
 * Configures how an SMPP server behaves during initial '''session bind''', i.e. when an SMPP client tries to authenticate
 * against a server.
 *
 * Note that - in line with a general philosophy followed in '''garuda''' - [[BindConfig]] serves not only as a
 * passive provider of configuration attributes like e.g. which [[Authenticator]] to use, what the `bind timeout` should
 * be and so forth. It also acts as a '''factory''' for resources needed during bind phase, most notably [[Timer]]s.
 *
 * @param authenticator The [[Authenticator]] instance to use during bind phase, must not be `null`
 * @param bindTimeoutMillis The bind timeout in milliseconds. Should a connected client fail to successfully authenticate
 *                          itself within `bindTimeoutMillis` the current [[io.garuda.server.spi.session.ServerSession]]
 *                          will be closed. Must be > 0.
 * @param bindTimerFactory Factory for [[Timer]] instances to be used during the bind phase. If `null` a default factory
 *                         will be used.
 * @param builderContext The [[BuilderContext]] to be used during bind phase, must not be `null`.
 * @throws IllegalArgumentException If `authenticator` is `null` OR If `bindTimeoutMillis` is <= 0 OR If `builderContext` is `null`
 *
 * @see [[Authenticator]]</br>
 *      [[Timer]]
 */
@throws[IllegalArgumentException]("If authenticator is null")
@throws[IllegalArgumentException]("If bindTimeoutMillis is <= 0")
@throws[IllegalArgumentException]("If builderContext is null")
class BindConfig(val authenticator: Authenticator,
                 val bindTimeoutMillis: Long,
                 bindTimerFactory: (BindConfig => Timer),
                 builderContext: BuilderContext) {
  require(authenticator != null, "Authenticator must not be null")
  require(bindTimeoutMillis > 0, s"Bind timeout needs to be > 0: ${bindTimeoutMillis}")
  require(builderContext != null, "Builder context must not be null")

  /**
   * [[Timer]] factory to use when users do not configure a custom factory. Always returns the same [[HashedWheelTimer]]
   * instance.
   *
   * '''IMPORTANT''' In case of many simultaneous bind requests re-using the same [[Timer]] instance for all those requests
   * might lead to rather imprecise timings.
   */
  val DefaultBindTimerFactory: (BindConfig => Timer) = Function.const(new HashedWheelTimer(new ThreadFactory {

    val nextThreadId: AtomicInteger = new AtomicInteger(1)

    override def newThread(r: Runnable): Thread = new Thread(r, "session-bind-timeout#" + nextThreadId.getAndIncrement)
  }))

  private[this] val bindTimerFac: (BindConfig => Timer) =
    if (bindTimerFactory != null) bindTimerFactory else DefaultBindTimerFactory

  private[this] val registeringBindTimerFactory: BindConfig => Timer =
    builderContext.resourcesRegistry.registerTimer[BindConfig](bindTimerFac)

  /**
   * Return a [[Timer]] instance to be used during bind phase, i.e. to shut down a [[io.netty.channel.Channel C h a n n e l]] in case
   * a predefined timeout elapses.
   *
   * The [[Timer]] instances returned from this method will be registered with the current [[io.garuda.common.resources.ResourcesRegistry]],
   * i.e. they will be stopped when [[io.garuda.common.resources.ResourcesRegistry]] is
   * [[io.garuda.common.resources.ResourcesRegistry.stop s t o p p e d]].
   *
   * @return [[Timer]] instance to be used during bind phase, never `null`
   */
  def bindTimer(): Timer = registeringBindTimerFactory(this)
}

/**
 * Companion object for [[BindConfig]]. Provides default values for [[BindConfig]]'s attributes, and a method for
 * constructing a default [[BindConfig]] given a [[BuilderContext]].
 */
object BindConfig {

  /**
   * Default [[Authenticator]] to be used when a user does not specify a custom [[Authenticator]]. This [[Authenticator]]
   * will simply reject all authentication requests and is thus only useful in some testing scenarios.
   */
  val DefaultAuthenticator: Authenticator = AlwaysNackAuthenticator

  /**
   * Default bind timeout in milliseconds (1000 ms).
   */
  val DefaultBindTimeoutMillis: Long = 1000L

  /**
   * Return a new default [[BindConfig]] instance that uses the supplied [[BuilderContext b u i l d e r C o n t e x t]].
   *
   * @param builderContext The [[BuilderContext]] to use
   * @throws IllegalArgumentException If `builderContext` is `null`
   * @return A new default [[BindConfig]] instance
   */
  @throws[IllegalArgumentException]("If builderContext is null")
  def default(builderContext: BuilderContext): BindConfig =
    new BindConfig(
      DefaultAuthenticator,
      DefaultBindTimeoutMillis,
      null,
      builderContext
    )
}

/**
 * Base trait for [[BindConfig]] builders. Supports hierarchical builders.
 *
 * @tparam T Self type, i.e. the concrete type of the class implementing this trait
 * @tparam X Type of the final product to create. For a non-hierarchical builder this will be [[BindConfig]]
 */
trait BindConfigBuilderSupport[T <: NodeBuilder[BindConfig, X], X] extends NodeBuilder[BindConfig, X] {
  self: T =>

  var authenticator: Authenticator = BindConfig.DefaultAuthenticator

  var bindTimeoutMillis: Long = BindConfig.DefaultBindTimeoutMillis

  var bindTimerFactory: (BindConfig => Timer) = null

  /**
   * The [[Authenticator]] to use during bind phase.
   *
   * @param authenticator The [[Authenticator]] to use during bind phase, must not be `null`
   * @throws IllegalArgumentException If `authenticator` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If authenticator is null")
  def authenticator(authenticator: Authenticator): T = {
    require(authenticator != null, "Authenticator must not be null")
    this.authenticator = authenticator
    this
  }

  /**
   * The bind timeout in milliseconds.
   *
   * @param timeout The bind timeout in milliseconds, must be > 0
   * @throws IllegalArgumentException If `timeout` is <= 0
   * @return this
   */
  @throws[IllegalArgumentException]("If timeout is <= 0")
  def bindTimeoutMillis(timeout: Long): T = {
    require(timeout > 0, "Bind timeout (ms) must be > 0: " + timeout)
    this.bindTimeoutMillis = timeout
    this
  }

  /**
   * The factory for [[Timer]]s to use during bind phase.
   *
   * @param timerFactory The factory for [[Timer]]s to use during bind phase, must not be `null`
   * @throws IllegalArgumentException If `timerFactory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If timerFactory is null")
  def bindTimerFactory(timerFactory: BindConfig => Timer): T = {
    require(timerFactory != null, "Bind timerFactory must not be null")
    this.bindTimerFactory = timerFactory
    this
  }

  @throws[IllegalArgumentException]("If builderContext is null")
  protected[this] def buildBindConfig(builderContext: BuilderContext): BindConfig = {
    new BindConfig(
      this.authenticator,
      this.bindTimeoutMillis,
      this.bindTimerFactory,
      builderContext)
  }

  @throws[IllegalArgumentException]("If builderContext is null")
  override def buildNode(builderContext: BuilderContext): BindConfig = buildBindConfig(builderContext)
}

/**
 * A builder for [[BindConfig]] instances.
 *
 * @param builderContext The [[BuilderContext]] to inject into all [[BindConfig]]s produced by this builder
 *
 * @see [[BindConfigBuilderSupport]]<br/>
 *      [[BindConfig]]
 */
final class BindConfigBuilder(builderContext: BuilderContext)
  extends BindConfigBuilderSupport[BindConfigBuilder, BindConfig] {

  override def build(): BindConfig = buildBindConfig(builderContext)
}

/**
 * Companion object for [[BindConfigBuilder]].
 */
object BindConfigBuilder {

  /**
   * Create a new [[BindConfigBuilder]] instance.
   *
   * @param builderContext The [[BuilderContext]] to inject into [[BindConfig]]
   * @throws IllegalArgumentException If `builderContext` is `null`
   * @return A new [[BindConfigBuilder]] instance
   */
  @throws[IllegalArgumentException]("If builderContext is null")
  def apply(builderContext: BuilderContext): BindConfigBuilder = new BindConfigBuilder(builderContext)
}
