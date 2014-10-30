package io.garuda.server.session

import io.garuda.common.builder.{BuilderContext, HasParentNodeBuilder, NodeBuilder}
import io.garuda.common.concurrent.CallerThreadExecutionContext
import io.garuda.common.session.ping.{PeriodicPingConfig, PeriodicPingConfigBuilderSupport}
import io.garuda.server.session.support.{AcknowledgingInboundPduRequestHandlerFactory, InstrumentedCallerThreadInboundPduRequestDispatcherFactory}
import io.garuda.server.spi.session.{InboundPduRequestDispatcher, InboundPduRequestDispatcherFactory, InboundPduRequestHandler, InboundPduRequestHandlerFactory}
import io.garuda.windowing.{WindowConfig, WindowConfigBuilderSupport}

import scala.concurrent.ExecutionContext

/**
 * Trait collecting all configuration options that are common [[CommonServerSessionConfig]] as well as
 * [[EffectiveServerSessionConfig]].
 *
 * @see [[io.garuda.server.spi.session.ServerSession]]
 */
sealed trait ServerSessionConfig {

  /**
   * Unique ID that connected clients use to identify this session.
   *
   * @return Unique ID that connected clients use to identify this session
   */
  def systemId: String

  /**
   * [[ExecutionContext]] used to run user code on. User code here is code run inside an [[InboundPduRequestHandler]].
   *
   * @return [[ExecutionContext]] used to run user code on. User code here is code run inside an [[InboundPduRequestHandler]]
   *
   * @see [[ExecutionContext]]
   */
  def userExecutionContext: ExecutionContext

  /**
   * [[BindConfig]] used to configure this session's behaviour during session bind.
   *
   * @return [[BindConfig]] used to configure this session's behaviour during session bind
   *
   * @see [[BindConfig]]
   */
  def bindConfig: BindConfig

  /**
   * [[PeriodicPingConfig]] used to configure this session ping behaviour.
   *
   * @return [[PeriodicPingConfig]] used to configure this session ping behaviour
   *
   * @see [[PeriodicPingConfig]]
   */
  def periodicPingConfig: PeriodicPingConfig

  /**
   * [[WindowConfig]] used to configure a session's '''inbound''' windowing.
   *
   * @return [[WindowConfig]] used to configure a session's '''inbound''' windowing
   *
   * @see [[WindowConfig]]
   */
  def inboundWindowConfig: WindowConfig

  /**
   * Factory used by a session to create its [[InboundPduRequestDispatcher]].
   *
   * @return Factory used by a session to create its [[InboundPduRequestDispatcher]]
   *
   * @see [[InboundPduRequestDispatcher]]
   */
  def inboundPduRequestDispatcherFactory: InboundPduRequestDispatcherFactory[_ <: InboundPduRequestDispatcher]

  /**
   * Factory used by a session to create its [[InboundPduRequestHandler]].
   *
   * @return Factory used by a session to create its [[InboundPduRequestHandler]]
   *
   * @see [[InboundPduRequestHandler]]
   */
  def inboundPduRequestHandlerFactory: InboundPduRequestHandlerFactory[_ <: InboundPduRequestHandler]

  /**
   * [[BuilderContext]] to be used by a session.
   *
   * @return [[BuilderContext]] to be used by a session
   *
   * @see [[BuilderContext]]
   */
  def builderContext: BuilderContext
}

/**
 * Configuration for an [[io.garuda.server.spi.session.ServerSession]] that is specific for an [[io.garuda.server.spi.authentication.AuthenticatedClient]],
 * i.e. that is not (necessarily) shared by all [[io.garuda.server.spi.session.ServerSession]]s but rather only
 * used by those associated with a specific remote user.
 *
 * @param periodicPingConfig Optional [[PeriodicPingConfig]] to override the default [[PeriodicPingConfig]]
 * @param inboundWindowConfig Optional [[WindowConfig]] to override the default [[WindowConfig]] used for '''inbound'''
 *                            windowing
 * @param inboundPduRequestHandlerFactory Optional [[InboundPduRequestHandlerFactory]] to override the default [[InboundPduRequestHandlerFactory]]
 */
case class SystemServerSessionConfig(periodicPingConfig: Option[PeriodicPingConfig],
                                     inboundWindowConfig: Option[WindowConfig],
                                     inboundPduRequestHandlerFactory: Option[InboundPduRequestHandlerFactory[_ <: InboundPduRequestHandler]])

/**
 * Companion object for [[SystemServerSessionConfig]].
 */
object SystemServerSessionConfig {

  /**
   * ''Undefined''[[SystemServerSessionConfig]], i.e. one that does not define any system specific settings.
   */
  val Undefined: SystemServerSessionConfig = SystemServerSessionConfig(None, None, None)
}

/**
 * [[io.garuda.server.spi.session.ServerSession]] configuration that is common to all [[io.garuda.server.spi.session.ServerSession]]s
 * created by an [[io.garuda.server.Server]].
 *
 * @param systemId Unique ID that connected clients use to identify this session
 * @param userExecutionContext [[ExecutionContext]] used to run user code on. User code here is code run inside an [[InboundPduRequestHandler]]
 * @param bindConfig [[BindConfig]] used to configure this session's behaviour during session bind
 * @param periodicPingConfig [[PeriodicPingConfig]] used to configure this session ping behaviour
 * @param inboundWindowConfig [[WindowConfig]] used to configure a session's '''inbound''' windowing
 * @param inboundPduRequestHandlerFactory Factory used by a session to create its [[InboundPduRequestHandler]]
 * @param builderContext [[BuilderContext]] to be used by a session
 * @throws IllegalArgumentException If one the parameters is `null`
 */
@throws[IllegalArgumentException]("If one of the parameters is null")
class CommonServerSessionConfig(val systemId: String,
                                val userExecutionContext: ExecutionContext,
                                val bindConfig: BindConfig,
                                val periodicPingConfig: PeriodicPingConfig,
                                val inboundWindowConfig: WindowConfig,
                                val inboundPduRequestDispatcherFactory: InboundPduRequestDispatcherFactory[_ <: InboundPduRequestDispatcher],
                                val inboundPduRequestHandlerFactory: InboundPduRequestHandlerFactory[_ <: InboundPduRequestHandler],
                                val builderContext: BuilderContext)
  extends ServerSessionConfig {
  require(systemId != null, "Argument 'id' must not be null")
  require(userExecutionContext != null, "Argument 'userExecutionContext' must not be null")
  require(bindConfig != null, "Argument 'bindConfig' must not be null")
  require(periodicPingConfig != null, "Argument 'periodicPingConfig' must not be null")
  require(inboundWindowConfig != null, "Argument 'inboundWindowConfig' must not be null")
  require(inboundPduRequestDispatcherFactory != null, "Argument 'inboundPduRequestDispatcherFactory' must not be null")
  require(inboundPduRequestHandlerFactory != null, "Argument 'inboundPduRequestHandlerFactory' must not be null")
  require(builderContext != null, "Argument 'builderContext' must not be null")

  /**
   * Given an optional [[SystemServerSessionConfig]], create a session's '''effective''' configuration. In the
   * [[ServerSessionConfig]] returned settings from `systemConfig` - if present - will override settings from this
   * [[CommonServerSessionConfig]].
   *
   * @param systemConfig The optional [[SystemServerSessionConfig]] with settings to override this [[CommonServerSessionConfig]]'s
   *                     settings
   * @return A session's '''effective''' [[ServerSessionConfig]]
   */
  def effectiveConfig(systemConfig: Option[SystemServerSessionConfig]): ServerSessionConfig =
    new EffectiveServerSessionConfig(systemConfig, this)
}

/**
 * Companion object for [[CommonServerSessionConfig]] defining default values for most of its parameters.
 */
object CommonServerSessionConfig {

  /**
   * Default `id`. Only useful for testing purposes.
   */
  val DefaultSystemId: String = "defaultCommonSmppServerSession"

  /**
   * The default [[ExecutionContext]] used for executing [[InboundPduRequestHandler]]s. The [[CallerThreadExecutionContext]].
   */
  val DefaultUserExecutionContext: ExecutionContext = CallerThreadExecutionContext

  val DefaultInboundPduRequestDispatcherFactory: InboundPduRequestDispatcherFactory[_ <: InboundPduRequestDispatcher] =
    InstrumentedCallerThreadInboundPduRequestDispatcherFactory

  /**
   * Default [[InboundPduRequestHandlerFactory]], the [[AcknowledgingInboundPduRequestHandlerFactory]].
   */
  val DefaultInboundPduRequestHandlerFactory: InboundPduRequestHandlerFactory[_ <: InboundPduRequestHandler] =
    AcknowledgingInboundPduRequestHandlerFactory

  /**
   * Create a default [[CommonServerSessionConfig]], using the supplied [[BuilderContext]].
   *
   * @param builderContext The [[BuilderContext]] to inject into the returned [[CommonServerSessionConfig]]
   * @return A new [[CommonServerSessionConfig]] instance, with most fields set to their default values
   */
  def default(builderContext: BuilderContext): CommonServerSessionConfig =
    new CommonServerSessionConfig(
      DefaultSystemId,
      DefaultUserExecutionContext,
      BindConfig.default(builderContext),
      PeriodicPingConfig.default(builderContext),
      WindowConfig.default(builderContext),
      DefaultInboundPduRequestDispatcherFactory,
      DefaultInboundPduRequestHandlerFactory,
      builderContext
    )
}

final class PeriodicPingConfigNodeBuilder[X, PB <: NodeBuilder[CommonServerSessionConfig, X]](protected[this] val parentNodeBuilder: PB)
  extends PeriodicPingConfigBuilderSupport[PeriodicPingConfigNodeBuilder[X, PB], X]
  with HasParentNodeBuilder[CommonServerSessionConfig, PB, PeriodicPingConfig, X] {

}

final class BindConfigNodeBuilder[X, PB <: NodeBuilder[CommonServerSessionConfig, X]](protected[this] val parentNodeBuilder: PB)
  extends BindConfigBuilderSupport[BindConfigNodeBuilder[X, PB], X]
  with HasParentNodeBuilder[CommonServerSessionConfig, PB, BindConfig, X] {

}

final class WindowConfigNodeBuilder[X, PB <: NodeBuilder[CommonServerSessionConfig, X]](protected[this] val parentNodeBuilder: PB)
  extends WindowConfigBuilderSupport[WindowConfigNodeBuilder[X, PB], X]
  with HasParentNodeBuilder[CommonServerSessionConfig, PB, WindowConfig, X] {

}

/**
 * Base trait for [[CommonServerSessionConfig]] builders.
 *
 * @tparam T Self type, i.e. the concrete type of the class implementing this trait
 * @tparam X The type of product to build
 */
trait CommonServerSessionConfigBuilderSupport[T <: NodeBuilder[CommonServerSessionConfig, X], X]
  extends NodeBuilder[CommonServerSessionConfig, X] {
  self: T =>

  var systemId: String = CommonServerSessionConfig.DefaultSystemId

  var userExecutionContext: ExecutionContext = CommonServerSessionConfig.DefaultUserExecutionContext

  var inboundPduRequestDispatcherFactory: InboundPduRequestDispatcherFactory[_ <: InboundPduRequestDispatcher] =
    CommonServerSessionConfig.DefaultInboundPduRequestDispatcherFactory

  var inboundPduRequestHandlerFactory: InboundPduRequestHandlerFactory[_ <: InboundPduRequestHandler] =
    CommonServerSessionConfig.DefaultInboundPduRequestHandlerFactory

  private[this] val _bindConfigBuilder: BindConfigNodeBuilder[X, T] = new BindConfigNodeBuilder[X, T](this)

  private[this] val _periodicPingConfigBuilder: PeriodicPingConfigNodeBuilder[X, T] = new
      PeriodicPingConfigNodeBuilder[X, T](this)

  private[this] val _inboundWindowConfigBuilder: WindowConfigNodeBuilder[X, T] = new WindowConfigNodeBuilder[X, T](this)

  /**
   * Unique ID that connected clients use to identify this session.
   *
   * @param systemId Unique ID that connected clients use to identify this session
   * @throws IllegalArgumentException If `id` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If id is null")
  def systemId(systemId: String): T = {
    require(systemId != null && !systemId.isEmpty, "SystemId must not be empty: " + systemId)
    this.systemId = systemId
    this
  }

  /**
   * [[ExecutionContext]] used to run user code on. User code here is code run inside an [[InboundPduRequestHandler]].
   *
   * @param exec [[ExecutionContext]] used to run user code on. User code here is code run inside an [[InboundPduRequestHandler]]
   * @throws IllegalArgumentException If exec is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If exec is null")
  def userExecutionContext(exec: ExecutionContext): T = {
    require(exec != null, "UserExecutionContext must not be null")
    this.userExecutionContext = exec
    this
  }

  /**
   * Factory used by a session to create its [[InboundPduRequestDispatcher]].
   *
   * @param factory Factory used by a session to create its [[InboundPduRequestDispatcher]]
   * @throws IllegalArgumentException If `factory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If factory is null")
  def inboundPduRequestDispatcherFactory(factory: InboundPduRequestDispatcherFactory[_ <: InboundPduRequestDispatcher]): T = {
    require(factory != null, "InboundPduRequestDispatcherFactory must not be null")
    this.inboundPduRequestDispatcherFactory = factory
    this
  }

  /**
   * Factory used by a session to create its [[InboundPduRequestHandler]].
   *
   * @param factory Factory used by a session to create its [[InboundPduRequestHandler]]
   * @throws IllegalArgumentException If `factory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If factory is null")
  def inboundPduRequestHandlerFactory(factory: InboundPduRequestHandlerFactory[_ <: InboundPduRequestHandler]): T = {
    require(factory != null, "InboundPduRequestHandlerFactory must not be null")
    this.inboundPduRequestHandlerFactory = factory
    this
  }

  /**
   * Return a [[BindConfigNodeBuilder]].
   *
   * @return A [[BindConfigNodeBuilder]]
   */
  def bindConfigBuilder(): BindConfigNodeBuilder[X, T] = _bindConfigBuilder

  /**
   * Return a [[PeriodicPingConfigNodeBuilder]].
   *
   * @return A [[PeriodicPingConfigNodeBuilder]]
   */
  def periodicPingConfigBuilder(): PeriodicPingConfigNodeBuilder[X, T] = _periodicPingConfigBuilder

  /**
   * Return a [[WindowConfigNodeBuilder]] to configure '''inbound''' windowing.
   *
   * @return A [[WindowConfigNodeBuilder]] to configure '''inbound''' windowing
   */
  def inboundWindowConfigBuilder(): WindowConfigNodeBuilder[X, T] = _inboundWindowConfigBuilder

  protected[this] def buildSessionConfig(builderContext: BuilderContext): CommonServerSessionConfig = {
    new CommonServerSessionConfig(
      this.systemId,
      this.userExecutionContext,
      _bindConfigBuilder.buildNode(builderContext),
      _periodicPingConfigBuilder.buildNode(builderContext),
      _inboundWindowConfigBuilder.buildNode(builderContext),
      this.inboundPduRequestDispatcherFactory,
      this.inboundPduRequestHandlerFactory,
      builderContext)
  }

  override def buildNode(builderContext: BuilderContext): CommonServerSessionConfig = buildSessionConfig(builderContext)
}

final class CommonServerSessionConfigBuilder(builderContext: BuilderContext)
  extends CommonServerSessionConfigBuilderSupport[CommonServerSessionConfigBuilder, CommonServerSessionConfig] {

  def build(): CommonServerSessionConfig = buildSessionConfig(builderContext)
}

/**
 * Companion object for [[CommonServerSessionConfigBuilder]].
 */
object CommonServerSessionConfigBuilder {

  /**
   * Create a new [[CommonServerSessionConfigBuilder]] instance that uses the supplied [[BuilderContext]].
   *
   * @param builderContext The [[BuilderContext]] to use
   * @return A new [[CommonServerSessionConfigBuilder]] instance
   */
  def apply(builderContext: BuilderContext): CommonServerSessionConfigBuilder = new
      CommonServerSessionConfigBuilder(builderContext)
}

/**
 * Fusing [[SystemServerSessionConfig]] into [[CommonServerSessionConfig]] yields the [[EffectiveServerSessionConfig]].
 * Note that in an [[EffectiveServerSessionConfig]] configuration options defined in [[SystemServerSessionConfig]]
 * take precedence over those defined in [[CommonServerSessionConfig]].
 *
 * @param systemConfig The system-specific session config
 * @param defaultConfig The common session config
 *
 * @see [[SystemServerSessionConfig]]<br/>
 *      [[CommonServerSessionConfig]]
 */
private[this] class EffectiveServerSessionConfig(systemConfig: SystemServerSessionConfig,
                                                 defaultConfig: CommonServerSessionConfig)
  extends ServerSessionConfig {

  def this(systemConfig: Option[SystemServerSessionConfig], defaultConfig: CommonServerSessionConfig) =
    this(systemConfig.getOrElse(SystemServerSessionConfig.Undefined), defaultConfig)

  /**
   * Unique ID that connected clients use to identify this session.
   *
   * @return Unique ID that connected clients use to identify this session
   */
  def systemId: String = defaultConfig.systemId

  /**
   * [[ExecutionContext]] used to run user code on. User code here is code run inside an [[InboundPduRequestHandler]].
   *
   * @return [[ExecutionContext]] used to run user code on. User code here is code run inside an [[InboundPduRequestHandler]]
   *
   * @see [[ExecutionContext]]
   */
  def userExecutionContext: ExecutionContext = defaultConfig.userExecutionContext

  /**
   * [[BindConfig]] used to configure this session's behaviour during session bind.
   *
   * @return [[BindConfig]] used to configure this session's behaviour during session bind
   *
   * @see [[BindConfig]]
   */
  def bindConfig: BindConfig = defaultConfig.bindConfig

  /**
   * [[PeriodicPingConfig]] used to configure this session ping behaviour.
   *
   * @return [[PeriodicPingConfig]] used to configure this session ping behaviour
   *
   * @see [[PeriodicPingConfig]]
   */
  def periodicPingConfig: PeriodicPingConfig = systemConfig.periodicPingConfig.getOrElse(defaultConfig.periodicPingConfig)

  /**
   * [[WindowConfig]] used to configure a session's '''inbound''' windowing.
   *
   * @return [[WindowConfig]] used to configure a session's '''inbound''' windowing
   *
   * @see [[WindowConfig]]
   */
  def inboundWindowConfig: WindowConfig = systemConfig.inboundWindowConfig.getOrElse(defaultConfig.inboundWindowConfig)

  /**
   * Factory used by a session to create its [[InboundPduRequestDispatcher]].
   *
   * @return Factory used by a session to create its [[InboundPduRequestDispatcher]]
   *
   * @see [[InboundPduRequestDispatcher]]
   */
  def inboundPduRequestDispatcherFactory: InboundPduRequestDispatcherFactory[_ <: InboundPduRequestDispatcher] =
    defaultConfig.inboundPduRequestDispatcherFactory

  /**
   * Factory used by a session to create its [[InboundPduRequestHandler]].
   *
   * @return Factory used by a session to create its [[InboundPduRequestHandler]]
   *
   * @see [[InboundPduRequestHandler]]
   */
  def inboundPduRequestHandlerFactory: InboundPduRequestHandlerFactory[_ <: InboundPduRequestHandler] =
    systemConfig.inboundPduRequestHandlerFactory.getOrElse(defaultConfig.inboundPduRequestHandlerFactory)

  /**
   * [[BuilderContext]] to be used by a session.
   *
   * @return [[BuilderContext]] to be used by a session
   *
   * @see [[BuilderContext]]
   */
  def builderContext: BuilderContext = defaultConfig.builderContext
}
