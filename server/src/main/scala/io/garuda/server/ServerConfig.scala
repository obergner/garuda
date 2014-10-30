package io.garuda.server

import java.net.{InetSocketAddress, SocketAddress}

import com.codahale.metrics.MetricRegistry
import io.garuda.common.builder.{BuilderContext, HasParentNodeBuilder, NodeBuilder}
import io.garuda.common.resources.ResourcesRegistry
import io.garuda.common.spi.session.Session
import io.garuda.server.session.support.{StateBasedServerSession, StateBasedServerSessionFactory}
import io.garuda.server.session.{CommonServerSessionConfig, CommonServerSessionConfigBuilderSupport}
import io.garuda.server.spi.session.SmppServerSessionFactory
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.{Channel, EventLoopGroup}

import scala.concurrent.duration._
import scala.language.{existentials, postfixOps}


/**
 * Configures an [[Server]].
 *
 * @param name Unique ''name'' of the [[Server]] to configure. Must not be `null`.
 * @param bindTo The [[SocketAddress]] the [[Server]] should listen on. Must not be `null`.
 * @param soReuseAddress
 * @param soKeepAlive
 * @param soBacklog
 * @param soLinger
 * @param soSendBufferSizeBytes
 * @param soReceiveBufferSizeBytes
 * @param tcpNoDelay
 * @param bossEventLoopGroupFactory Factory for the ''boss'' [[EventLoopGroup]]. Must not be `null`.
 * @param workerEventLoopGroupFactory Factory for the ''worker'' [[EventLoopGroup]]. Must not be `null`.
 * @param sessionFactory [[SmppServerSessionFactory]] to use for creating new [[io.garuda.common.spi.session.Session]]s. Must not be `null`.
 * @param commonSessionConfig [[CommonServerSessionConfig]] used to configure new [[io.garuda.common.spi.session.Session]]s. Must not
 *                            be `null`.
 * @param builderContext [[BuilderContext]] to use. Must not be `null`.
 * @throws IllegalArgumentException If one of the required parameters is `null`.
 *
 * @see [[EventLoopGroup]]<br/>
 *      [[SmppServerSessionFactory]]<br/>
 *      [[CommonServerSessionConfig]]<br/>
 *      [[BuilderContext]]
 */
@throws[IllegalArgumentException]("If one of the required parameters is null")
final class ServerConfig(val name: String,
                         val bindTo: SocketAddress,
                         val soReuseAddress: Option[java.lang.Boolean],
                         val soKeepAlive: Option[java.lang.Boolean],
                         val soBacklog: Option[java.lang.Integer],
                         val soLinger: Option[java.lang.Integer],
                         val soSendBufferSizeBytes: Option[java.lang.Integer],
                         val soReceiveBufferSizeBytes: Option[java.lang.Integer],
                         val tcpNoDelay: Option[java.lang.Boolean],
                         private[this] val bossEventLoopGroupFactory: () => EventLoopGroup,
                         private[this] val workerEventLoopGroupFactory: () => EventLoopGroup,
                         private[this] val sessionFactory: SmppServerSessionFactory[_ <: Session],
                         val commonSessionConfig: CommonServerSessionConfig,
                         val builderContext: BuilderContext) {
  require(name != null, "Argument 'name' must not be null")
  require(bindTo != null, "Argument 'bindTo' must not be null")
  require(bossEventLoopGroupFactory != null, "Argument 'bossEventLoopGroupFactory' must not be null")
  require(workerEventLoopGroupFactory != null, "Argument 'workerEventLoopGroupFactory' must not be null")
  require(commonSessionConfig != null, "Argument 'commonSessionConfig' must not be null")
  require(builderContext != null, "Argument 'builderContext' must not be null")

  val metricRegistry: MetricRegistry = builderContext.metricRegistry

  lazy val bossEventLoopGroup: EventLoopGroup = bossEventLoopGroupFactory()

  lazy val workerEventLoopGroup: EventLoopGroup = workerEventLoopGroupFactory()

  val serverSessionFactory: Channel => Session = sessionFactory(_, this.commonSessionConfig)

  override def toString(): String = {
    s"ServerConfig(name:${name}," +
      s"bindTo:${bindTo}," +
      s"soReuseAddr:${soReuseAddress}," +
      s"soKeepAlive:${soKeepAlive}," +
      s"soBacklog:${soBacklog}," +
      s"soLinger:${soLinger}," +
      s"soSendBufferSizeBytes:${soSendBufferSizeBytes}," +
      s"soReceiveBufferSizeBytes:${soReceiveBufferSizeBytes}," +
      s"tcpNoDelay:${tcpNoDelay}," +
      s"sessionFactory:${sessionFactory}," +
      s"commonSessionConfig:${commonSessionConfig}"
  }
}

/**
 * Factory for [[ServerConfig]]s, providing default values for most of its parameters.
 */
object ServerConfig {

  /**
   * Default port to use, 2775.
   */
  val DefaultPort: Int = 2775

  /**
   * Default [[ServerConfig.name]] to use. Only useful for testing.
   */
  val DefaultName: String = "defaultSmppServer"

  /**
   * Default [[ServerConfig.soReuseAddress]] to use, `None`.
   */
  val DefaultSoReuseAddress: Option[java.lang.Boolean] = None

  /**
   * Default [[ServerConfig.soKeepAlive]] to use, `None`.
   */
  val DefaultSoKeepAlive: Option[java.lang.Boolean] = None

  /**
   * Default [[ServerConfig.soBacklog]] to use, `None`.
   */
  val DefaultSoBacklog: Option[java.lang.Integer] = None

  /**
   * Default [[ServerConfig.soLinger]] to use, `None`.
   */
  val DefaultSoLinger: Option[java.lang.Integer] = None

  /**
   * Default [[ServerConfig.soSendBufferSizeBytes]] to use, `None`.
   */
  val DefaultSoSendBufferSizeBytes: Option[java.lang.Integer] = None

  /**
   * Default [[ServerConfig.soReceiveBufferSizeBytes]] to use, `None`.
   */
  val DefaultSoReceiveBufferSizeBytes: Option[java.lang.Integer] = None

  /**
   * Default [[ServerConfig.tcpNoDelay]] to use, `None`.
   */
  val DefaultTcpNoDelay: Option[java.lang.Boolean] = None

  /**
   * Default factory for the [[MetricRegistry]] to be used for publishing metrics. Will simply return a vanilla
   * [[MetricRegistry]] with no special configuration.
   */
  val DefaultMetricRegistryFactory: () => MetricRegistry = () => {
    val metricRegistry = new MetricRegistry()
    //JmxReporter.forRegistry(metricRegistry).build().start()
    metricRegistry
  }

  /**
   * Default [[ServerConfig.bossEventLoopGroupFactory]] to use. Simply returns a new [[NioEventLoopGroup]] on each
   * call.
   */
  val DefaultBossEventLoopGroupFactory: () => EventLoopGroup = () => {
    new NioEventLoopGroup
  }

  /**
   * Default [[ServerConfig.workerEventLoopGroupFactory]] to use. Simply returns a new [[NioEventLoopGroup]] on each
   * call.
   */
  val DefaultWorkerEventLoopGroupFactory: () => EventLoopGroup = () => {
    new NioEventLoopGroup
  }

  /**
   * Default [[ServerConfig.sessionFactory]] to use, [[StateBasedServerSessionFactory]].
   */
  val DefaultSmppServerSessionFactory: SmppServerSessionFactory[StateBasedServerSession] = StateBasedServerSessionFactory

  /**
   * Create and return a default [[BuilderContext]], using a [[MetricRegistry]] created by our [[DefaultMetricRegistryFactory]]
   * and a plain [[ResourcesRegistry]].
   *
   * @return Create and return a default [[BuilderContext]]
   */
  def defaultBuilderContext(): BuilderContext = {
    val metricReg = DefaultMetricRegistryFactory()
    val resourcesReg = new ResourcesRegistry(DefaultName, 10 milliseconds, metricReg)
    BuilderContext(metricReg, resourcesReg)
  }

  /**
   * Create and return a new default [[ServerConfig]] instance. This should only be used for testing.
   *
   * @return A new default [[ServerConfig]] instance
   */
  def Default: ServerConfig = {
    val builderCtx = defaultBuilderContext()
    new ServerConfig(DefaultName,
      new InetSocketAddress(ServerConfig.DefaultPort),
      DefaultSoReuseAddress,
      DefaultSoKeepAlive,
      DefaultSoBacklog,
      DefaultSoLinger,
      DefaultSoSendBufferSizeBytes,
      DefaultSoReceiveBufferSizeBytes,
      DefaultTcpNoDelay,
      DefaultBossEventLoopGroupFactory,
      DefaultWorkerEventLoopGroupFactory,
      DefaultSmppServerSessionFactory,
      CommonServerSessionConfig.default(builderCtx),
      builderCtx
    )
  }

  /**
   * Create a new [[ServerConfig]] instance from the supplied parameters.
   *
   * @param name Unique ''name'' of the [[Server]] to configure. Must not be `null`.
   * @param bindTo The [[SocketAddress]] the [[Server]] should listen on. Must not be `null`.
   * @param soReuseAddress
   * @param soKeepAlive
   * @param soBacklog
   * @param soLinger
   * @param soSendBufferSizeBytes
   * @param soReceiveBufferSizeBytes
   * @param tcpNoDelay
   * @param bossEventLoopGroupFactory Factory for the ''boss'' [[EventLoopGroup]]. Must not be `null`.
   * @param workerEventLoopGroupFactory Factory for the ''worker'' [[EventLoopGroup]]. Must not be `null`.
   * @param sessionFactory [[SmppServerSessionFactory]] to use for creating new [[Session]]s. Must not be `null`.
   * @param commonSessionConfig [[CommonServerSessionConfig]] used to configure new [[Session]]s. Must not
   *                            be `null`.
   * @param builderContext [[BuilderContext]] to use. Must not be `null`.
   * @throws If one of the required parameters is `null`
   * @return A new [[ServerConfig]] instance built from the supplied parameters
   */
  @throws[IllegalArgumentException]("If one of the required parameters is null")
  def apply(name: String,
            bindTo: SocketAddress,
            soReuseAddress: Option[java.lang.Boolean],
            soKeepAlive: Option[java.lang.Boolean],
            soBacklog: Option[java.lang.Integer],
            soLinger: Option[java.lang.Integer],
            soSendBufferSizeBytes: Option[java.lang.Integer],
            soReceiveBufferSizeBytes: Option[java.lang.Integer],
            tcpNoDelay: Option[java.lang.Boolean],
            bossEventLoopGroupFactory: () => EventLoopGroup,
            workerEventLoopGroupFactory: () => EventLoopGroup,
            sessionFactory: SmppServerSessionFactory[_ <: Session],
            commonSessionConfig: CommonServerSessionConfig,
            builderContext: BuilderContext): ServerConfig = {
    new ServerConfig(name,
      bindTo,
      soReuseAddress,
      soKeepAlive,
      soBacklog,
      soLinger,
      soSendBufferSizeBytes,
      soReceiveBufferSizeBytes,
      tcpNoDelay,
      bossEventLoopGroupFactory,
      workerEventLoopGroupFactory,
      sessionFactory,
      commonSessionConfig,
      builderContext)
  }
}

final class CommonServerSessionConfigNodeBuilder[X, PB <: NodeBuilder[ServerConfig, X]](protected[this] val parentNodeBuilder: PB,
                                                                                        val builderContext: BuilderContext)
  extends CommonServerSessionConfigBuilderSupport[CommonServerSessionConfigNodeBuilder[X, PB], X]
  with HasParentNodeBuilder[ServerConfig, PB, CommonServerSessionConfig, X] {

}

/**
 * Base trait for [[ServerConfig]] builders, supports hierarchical builders.
 *
 * @tparam T Self type, i.e. the concrete type of the class implementing this trait.
 * @tparam X The type of product to build. For non-hierarchical builders this will be [[ServerConfig]].
 */
trait ServerConfigBuilderSupport[T <: NodeBuilder[ServerConfig, X], X]
  extends NodeBuilder[ServerConfig, X] {
  self: T =>

  private[this] var name: String = ServerConfig.DefaultName

  private[this] var bindTo: SocketAddress = new InetSocketAddress(ServerConfig.DefaultPort)

  private[this] var soReuseAddress: Option[java.lang.Boolean] = ServerConfig.DefaultSoReuseAddress

  private[this] var soKeepAlive: Option[java.lang.Boolean] = ServerConfig.DefaultSoKeepAlive

  private[this] var soBacklog: Option[java.lang.Integer] = ServerConfig.DefaultSoBacklog

  private[this] var soLinger: Option[java.lang.Integer] = ServerConfig.DefaultSoLinger

  private[this] var soSendBufferSizeBytes: Option[java.lang.Integer] = ServerConfig.DefaultSoSendBufferSizeBytes

  private[this] var soReceiveBufferSizeBytes: Option[java.lang.Integer] = ServerConfig.DefaultSoReceiveBufferSizeBytes

  private[this] var tcpNoDelay: Option[java.lang.Boolean] = ServerConfig.DefaultTcpNoDelay

  private[this] var metricRegistryFactory: () => MetricRegistry = ServerConfig.DefaultMetricRegistryFactory

  private[this] var bossEventLoopGroupFactory: () => EventLoopGroup = ServerConfig.DefaultBossEventLoopGroupFactory

  private[this] var workerEventLoopGroupFactory: () => EventLoopGroup = ServerConfig.DefaultWorkerEventLoopGroupFactory

  private[this] var sessionFactory: SmppServerSessionFactory[_ <: Session] = ServerConfig.DefaultSmppServerSessionFactory

  private[this] lazy val _sessionConfigBuilder: CommonServerSessionConfigNodeBuilder[X, T] = new
      CommonServerSessionConfigNodeBuilder[X, T](this, _builderContext)

  private[this] lazy val _builderContext: BuilderContext = {
    val metricReg = metricRegistryFactory()
    val resourcesReg = new ResourcesRegistry(this.name, 100 milliseconds, metricReg)
    BuilderContext(metricReg, resourcesReg)
  }

  /**
   * Unique ''name'' of the [[Server]] to configure. Must not be `null`.
   *
   * @param name Unique ''name'' of the [[Server]] to configure. Must not be `null`.
   * @throws IllegalArgumentException If `name` is `null` or empty
   * @return this
   */
  @throws[IllegalArgumentException]("If name is null or empty")
  def name(name: String): T = {
    require(name != null && !name.isEmpty, "Server name must not be empty: " + name)
    this.name = name
    this
  }

  /**
   * The [[SocketAddress]] the [[Server]] should listen on. Must not be `null`.
   *
   * @param bindTo The [[SocketAddress]] the [[Server]] should listen on. Must not be `null`.
   * @throws IllegalArgumentException If `bindTo` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If bindTo is null")
  def bindTo(bindTo: SocketAddress): T = {
    require(bindTo != null, "Bind address must not be null")
    this.bindTo = bindTo
    this
  }

  /**
   *
   * @param reuseAddr
   * @return
   */
  def soReuseAddress(reuseAddr: java.lang.Boolean): T = {
    this.soReuseAddress = Some(reuseAddr)
    this
  }

  /**
   *
   * @param keepAlive
   * @return
   */
  def soKeepAlive(keepAlive: java.lang.Boolean): T = {
    this.soKeepAlive = Some(keepAlive)
    this
  }

  /**
   *
   * @param backlog
   * @throws IllegalArgumentException If `backlog` is `null` or <= 0
   * @return this
   */
  @throws[IllegalArgumentException]("If backlog is null or <= 0")
  def soBacklog(backlog: java.lang.Integer): T = {
    require(backlog != null && backlog > 0, s"Backlog needs to be > 0: ${backlog}")
    this.soBacklog = Some(backlog)
    this
  }

  /**
   *
   * @param linger
   * @throws IllegalArgumentException If `linger` is null
   * @return this
   */
  @throws[IllegalArgumentException]("If linger is null")
  def soLinger(linger: java.lang.Integer): T = {
    require(linger != null, "Argument 'linger' must not be null")
    this.soLinger = Some(linger)
    this
  }

  /**
   *
   * @param size
   * @throws IllegalArgumentException If `size` is `null` or <= 0
   * @return this
   */
  @throws[IllegalArgumentException]("If size is null or <= 0")
  def soSendBufferSizeBytes(size: java.lang.Integer): T = {
    require(size != null && size > 0, s"SoSendBufferSizeBytes needs to be > 0: ${size}")
    this.soSendBufferSizeBytes = Some(size)
    this
  }

  /**
   *
   * @param size
   * @throws IllegalArgumentException If `size` is `null` or <= 0
   * @return this
   */
  @throws[IllegalArgumentException]("If size is null or <= 0")
  def soReceiveBufferSizeBytes(size: java.lang.Integer): T = {
    require(size != null && size > 0, s"SoReceiveBufferSizeBytes needs to be > 0: ${size}")
    this.soReceiveBufferSizeBytes = Some(size)
    this
  }

  /**
   *
   * @param noDelay
   * @throws IllegalArgumentException If `noDelay` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If noDelay is null")
  def tcpNoDelay(noDelay: java.lang.Boolean): T = {
    require(noDelay != null, "Argument 'noDelay' must not be null")
    this.tcpNoDelay = Some(noDelay)
    this
  }

  /**
   * Factory for [[MetricRegistry]]. Must not be `null`.
   *
   * @param metricRegistryFactory Factory for [[MetricRegistry]]. Must not be `null`.
   * @throws IllegalArgumentException If `metricRegistryFactory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If metricRegistryFactory is null")
  def metricRegistryFactory(metricRegistryFactory: () => MetricRegistry): T = {
    require(metricRegistryFactory != null, "MetricRegisryFactory must not be null")
    this.metricRegistryFactory = metricRegistryFactory
    this
  }

  /**
   * Factory for the ''boss'' [[EventLoopGroup]]. Must not be `null`.
   *
   * @param eventLoopGroupFactory Factory for the ''boss'' [[EventLoopGroup]]. Must not be `null`.
   * @throws IllegalArgumentException If `eventLoopGroupFactory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If eventLoopGroupFactory is null")
  def bossEventLoopGroupFactory(eventLoopGroupFactory: () => EventLoopGroup): T = {
    require(eventLoopGroupFactory != null, "Boss EventLoopGroup factory must not be null")
    this.bossEventLoopGroupFactory = eventLoopGroupFactory
    this
  }

  /**
   * Factory for the ''worker'' [[EventLoopGroup]]. Must not be `null`.
   *
   * @param eventLoopGroupFactory Factory for the ''worker'' [[EventLoopGroup]]. Must not be `null`.
   * @throws IllegalArgumentException If `eventLoopGroupFactory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If eventLoopGroupFactory is null")
  def workerEventLoopGroupFactory(eventLoopGroupFactory: () => EventLoopGroup): T = {
    require(eventLoopGroupFactory != null, "Worker EventLoopGroup factory must not be null")
    this.workerEventLoopGroupFactory = eventLoopGroupFactory
    this
  }

  /**
   * [[SmppServerSessionFactory]] to use for creating new [[Session]]s. Must not be `null`.
   *
   * @param sessionFactory [[SmppServerSessionFactory]] to use for creating new [[Session]]s. Must not be `null`.
   * @throws IllegalArgumentException If `sessionFactory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If sessionFactory is null")
  def sessionFactory(sessionFactory: SmppServerSessionFactory[_ <: Session]): T = {
    require(sessionFactory != null, "Session factory must not be null")
    this.sessionFactory = sessionFactory
    this
  }

  /**
   * Return a [[CommonServerSessionConfigNodeBuilder]].
   *
   * @return Return a [[CommonServerSessionConfigNodeBuilder]]
   */
  def sessionConfigBuilder(): CommonServerSessionConfigNodeBuilder[X, T] = _sessionConfigBuilder

  protected[this] def buildServerConfig(): ServerConfig = {
    ServerConfig(
      name,
      bindTo,
      soReuseAddress,
      soKeepAlive,
      soBacklog,
      soLinger,
      soSendBufferSizeBytes,
      soReceiveBufferSizeBytes,
      tcpNoDelay,
      bossEventLoopGroupFactory,
      workerEventLoopGroupFactory,
      sessionFactory,
      sessionConfigBuilder().buildNode(_builderContext),
      _builderContext)
  }

  /**
   * Create a new [[ServerConfig]] instance.
   *
   * @param builderContext The [[BuilderContext]] to be used by the returned [[ServerConfig]].
   * @throws IllegalArgumentException If `builderContext` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If builderContext is null")
  override def buildNode(builderContext: BuilderContext): ServerConfig = buildServerConfig()
}

/**
 * A builder for [[ServerConfig]]s.
 *
 * @see [[ServerConfigBuilderSupport]]
 */
final class ServerConfigBuilder extends ServerConfigBuilderSupport[ServerConfigBuilder, ServerConfig] {

  /**
   * Build a new [[ServerConfig]] instance.
   *
   * @return A new [[ServerConfig]] instance
   */
  def build(): ServerConfig = buildServerConfig()
}

/**
 * Factory for [[ServerConfigBuilder]]s.
 */
object ServerConfigBuilder {

  /**
   * Create a new [[ServerConfigBuilder]] instance.
   *
   * @return A new [[ServerConfigBuilder]] instance
   */
  def apply(): ServerConfigBuilder = new ServerConfigBuilder
}
