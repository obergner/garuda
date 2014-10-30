package io.garuda.windowing

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import io.garuda.codec.pdu.PduResponse
import io.garuda.common.builder.{BuilderContext, NodeBuilder}
import io.garuda.common.concurrent.CallerThreadExecutionContext
import io.garuda.windowing.support.SemaphoreGuardedWindowFactory

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
 * Configures [[Window]]s - either inbound our outbound - for `sessions`. Doubles as a factory for [[Window]]s.
 *
 * @param capacity Maximum number of [[Slot]]s a [[Window]] may hold. Must be > 0.
 * @param slotAcquisitionTimeout A thread trying to acquire a [[Slot]] will wait up to `slotAcquisitionTimeout` for a
 *                               free [[Slot]] to become available. If none becomes available within this timeout the
 *                               calling thread will throw a [[SlotAcquisitionTimedOutException]]. Must not be `null`.
 * @param slotExpirationTimeout A [[io.garuda.codec.pdu.PduRequest]] will be stored for up to `slotExpirationTimeout`
 *                              in a [[Slot]] before that [[Slot]] '''expires'''. If there is no [[PduResponse]] within
 *                              this timeout the [[io.garuda.codec.pdu.PduRequest]] will be discarded. Must not be
 *                              `null`.
 * @param slotExpirationMonitoringInterval Interval with which a [[Window]] will be monitored for expired [[Slot]]s.
 *                                         Must not be `null`.
 * @param slotExpirationMonitorExecutorFactory A factory for [[ScheduledExecutorService]]s. A [[Window]] needs a
 *                                             [[ScheduledExecutorService]] to regularly check for expired [[Slot]]s.
 *                                             If `null` a default factory will be used.
 * @param slotCompletionCallbacks A [[immutable.Set]] of callbacks that will be informed whenever a [[Slot]] is '''completed''',
 *                                i.e. is freed with a [[PduResponse]], expires, or is closed. Must not be `null`.
 * @param slotCompletionExecutionContext [[ExecutionContext]] to be used for executing `slotCompletionCallbacks`. Must
 *                                       not be `null`.
 * @param windowFactory A factory for [[Window]]s given a [[WindowConfig]]. This [[WindowConfig]] instance will at runtime
 *                      use `windowFactory` to create new [[Window]] instances. Must not be `null`.
 *
 * @throws IllegalArgumentException If capacity <= 0 ||
 *                                  slotAcquisitionTimeout == null ||
 *                                  slotExpirationTimeout == null ||
 *                                  slotExpirationMonitoringInterval == null ||
 *                                  slotCompletionCallbacks == null ||
 *                                  slotCompletionExecutionContext == null ||
 *                                  windowFactory == null  ||
 *                                  If builderContext is null
 */
@throws[IllegalArgumentException]("If capacity <= 0 || slotAcquisitionTimeout == null || " +
  "slotExpirationTimeout == null || slotExpirationMonitoringInterval == null || slotCompletionCallbacks == null " +
  "|| slotCompletionExecutionContext == null || windowFactory == null  || If builderContext is null")
class WindowConfig(val capacity: Int,
                   val slotAcquisitionTimeout: Duration,
                   val slotExpirationTimeout: Duration,
                   val slotExpirationMonitoringInterval: Duration,
                   slotExpirationMonitorExecutorFactory: WindowConfig => ScheduledExecutorService,
                   val slotCompletionCallbacks: immutable.Set[Try[PduResponse] => Unit],
                   val slotCompletionExecutionContext: ExecutionContext,
                   private[this] val windowFactory: WindowFactory[_ <: Window],
                   builderContext: BuilderContext) {
  require(capacity > 0, s"Capacity needs to be > 0: ${capacity}")
  require(slotAcquisitionTimeout != null, "slotAcquisitionTimeout must not be null")
  require(slotExpirationTimeout != null, "slotExpirationTimeout must not be null")
  require(slotExpirationMonitoringInterval != null, "slotExpirationMonitoringInterval must not be null")
  require(slotCompletionCallbacks != null, "slotCompletionCallbacks must not be null")
  require(slotCompletionExecutionContext != null, "slotCompletionExecutionContext must not be null")
  require(windowFactory != null, "windowFactory must not be null")
  require(builderContext != null, "builderContext must not be null")

  /**
   * Factory for [[ScheduledExecutorService]]s that will be used by default when none other is passed explicitly. Always
   * creates a new single-threaded [[ScheduledExecutorService]].
   */
  val DefaultSlotExpirationMonitorExecutorFactory: WindowConfig => ScheduledExecutorService = _ => Executors.newSingleThreadScheduledExecutor(new
      ThreadFactory {
    val nextThreadId = new AtomicLong(0)

    override def newThread(r: Runnable): Thread = {
      new Thread(r, "slot-expiration-monitor#" + nextThreadId.incrementAndGet())
    }
  })

  private[this] val slotExpirationMonitorExecutorFac: WindowConfig => ScheduledExecutorService =
    if (slotExpirationMonitorExecutorFactory != null) slotExpirationMonitorExecutorFactory else DefaultSlotExpirationMonitorExecutorFactory

  private[this] val registeringSlotExpirationMonitorExecutorFactory: WindowConfig => ScheduledExecutorService =
    builderContext.resourcesRegistry.registerExecutorService[ScheduledExecutorService, WindowConfig](slotExpirationMonitorExecutorFac)

  /**
   * Creates new [[ScheduledExecutorService]]s to run tasks that monitor [[Window]]s for expired [[Slot]]s.
   *
   * '''Note''' that all [[ScheduledExecutorService]]s returned from this method will be registered with the current
   * [[io.garuda.common.resources.ResourcesRegistry]].
   *
   * @return A new/cached [[ScheduledExecutorService]], '''never''' `null`
   */
  def slotExpirationMonitorExecutor(): ScheduledExecutorService = registeringSlotExpirationMonitorExecutorFactory(this)

  /**
   * Create a new [[Window]] from this [[WindowConfig]] instance, using the supplied id as the new [[Window]]'s id.
   *
   * @param id ID of the [[Window]] to create
   * @throws IllegalArgumentException If id is `null`.
   * @return A new [[Window]] instance, configured from this [[WindowConfig]]. Will '''never''' be `null`.
   */
  @throws[IllegalArgumentException]("If id is null")
  def window(id: String): Window = windowFactory(id, this, builderContext.metricRegistry)
}

/**
 * Companion object for [[WindowConfig]]. Defines default values for most attributes, and provides a method for creating
 * default [[WindowConfig]] instances given a [[BuilderContext]].
 */
object WindowConfig {

  /**
   * Default maximum number of [[Slot]]s per [[Window]] (10).
   */
  val DefaultCapacity: Int = 10

  /**
   * Default [[Slot]] acquisition timeout (10 ms).
   */
  val DefaultSlotAcquisitionTimeout: Duration = 10 milliseconds

  /**
   * Default [[Slot]] expiration timeout (1 second).
   */
  val DefaultSlotExpirationTimeout: Duration = 1 second

  /**
   * Default [[Slot]] expiration monitoring interval (1 second).
   */
  val DefaultSlotExpirationMonitoringInterval: Duration = 1 second

  /**
   * Default set of [[Slot]] completion callbacks (empty set).
   */
  val DefaultSlotCompletionCallbacks: immutable.Set[Try[PduResponse] => Unit] = immutable.Set.empty[Try[PduResponse] => Unit]

  /**
   * By default [[Slot]] completion callbacks will be executed by this [[ExecutionContext]], the [[CallerThreadExecutionContext]],
   * i.e. callbacks will be executed on the same thread that completed a [[Slot]].
   */
  val DefaultSlotCompletionExecutionContext: ExecutionContext = CallerThreadExecutionContext

  /**
   * Default [[WindowFactory]] to use when creating new [[Window]] instances ([[SemaphoreGuardedWindowFactory]].
   */
  val DefaultWindowFactory: WindowFactory[_ <: Window] = SemaphoreGuardedWindowFactory

  /**
   * Create a default [[WindowConfig]], injecting the supplied [[BuilderContext]] into it.
   *
   * @param builderContext The [[BuilderContext]] to inject into the returned [[WindowConfig]]
   * @throws IllegalArgumentException If a required parameter is `null` or otherwise illegal
   * @return A default [[WindowConfig]] instance
   */
  @throws[IllegalArgumentException]("If a rqequired parameter is null or otherwise illegal")
  def default(builderContext: BuilderContext): WindowConfig = new WindowConfig(DefaultCapacity,
    DefaultSlotAcquisitionTimeout,
    DefaultSlotExpirationTimeout,
    DefaultSlotExpirationMonitoringInterval,
    null,
    DefaultSlotCompletionCallbacks,
    DefaultSlotCompletionExecutionContext,
    DefaultWindowFactory,
    builderContext)
}

/**
 * Base trait for [[WindowConfig]] builders. Supports hierarchical builders.
 *
 * @tparam T Self type, i.e. the concrete type of the class implementing this trait
 * @tparam X Type of product to create. In case of non-hierarchical builders this will be [[WindowConfig]]
 */
trait WindowConfigBuilderSupport[T <: NodeBuilder[WindowConfig, X], X] extends NodeBuilder[WindowConfig, X] {
  self: T =>

  import io.garuda.windowing.WindowConfig._

  var capacity: Int = DefaultCapacity

  var slotAcquisitionTimeout: Duration = DefaultSlotAcquisitionTimeout

  var slotExpirationTimeout: Duration = DefaultSlotExpirationTimeout

  var slotExpirationMonitorExecutorFactory: WindowConfig => ScheduledExecutorService = null

  var slotExpirationMonitoringInterval: Duration = DefaultSlotExpirationMonitoringInterval

  var slotCompletionCallbacks: immutable.Set[Try[PduResponse] => Unit] = DefaultSlotCompletionCallbacks

  var slotCompletionExecutionContext: ExecutionContext = DefaultSlotCompletionExecutionContext

  var windowFactory: WindowFactory[_ <: Window] = DefaultWindowFactory

  /**
   * Maximum number of [[Slot]]s in a [[Window]].
   *
   * @param capacity Maximum number of [[Slot]]s in a [[Window]], must be > 0
   * @throws IllegalArgumentException If `capacity` <= 0
   * @return this
   */
  @throws[IllegalArgumentException]("If capacity <= 0")
  def capacity(capacity: Int): T = {
    require(capacity > 0, s"Capacity needs to be > 0: ${capacity}")
    this.capacity = capacity
    this
  }

  /**
   * Timeout for threads attempting to acquire a [[Slot]] for a [[io.garuda.codec.pdu.PduRequest]].
   *
   * @param timeout Timeout for threads attempting to acquire a [[Slot]] for a [[io.garuda.codec.pdu.PduRequest]]
   * @throws IllegalArgumentException If `timeout` is null
   * @return this
   */
  @throws[IllegalArgumentException]("If timeout is null")
  def slotAcquisitionTimeout(timeout: Duration): T = {
    require(timeout != null, "SlotAcquisitionTimeout must not be null")
    this.slotAcquisitionTimeout = timeout
    this
  }

  /**
   * Timeout for [[Slot]]s waiting to be resolved with a [[PduResponse]].
   *
   * @param timeout Timeout for [[Slot]]s waiting to be resolved with a [[PduResponse]]
   * @throws IllegalArgumentException If `timeout` is null
   * @return this
   */
  @throws[IllegalArgumentException]("If timeout is null")
  def slotExpirationTimeout(timeout: Duration): T = {
    require(timeout != null, "SlotExpirationTimeout must not be null")
    this.slotExpirationTimeout = timeout
    this
  }

  /**
   * Factory for [[ScheduledExecutorService]]s that execute [[Slot]] expiration monitor tasks.
   *
   * @param factory Factory for [[ScheduledExecutorService]]s that execute [[Slot]] expiration monitor tasks
   * @throws IllegalArgumentException If `factory` is null
   * @return this
   */
  @throws[IllegalArgumentException]("If factory is null")
  def slotExpirationMonitorExecutorFactory(factory: WindowConfig => ScheduledExecutorService): T = {
    require(factory != null, "SlotExpirationMonitorExecutorFactory must not be null")
    this.slotExpirationMonitorExecutorFactory = factory
    this
  }

  /**
   * The interval used to run a [[Window]]'s [[Slot]] expiration monitor in.
   *
   * @param interval The interval used to run a [[Window]]'s [[Slot]] expiration monitor in
   * @throws IllegalArgumentException If interval is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If interval is null")
  def slotExpirationMonitoringInterval(interval: Duration): T = {
    require(interval != null, "SlotExpirationMonitoringInterval must not be null")
    this.slotExpirationMonitoringInterval = interval
    this
  }

  /**
   * Set of callbacks to invoke when a [[Slot]] is completed, either normally with a [[PduResponse]] or abnormally upon
   * expiration or close.
   *
   * @param callbacks Set of callbacks to invoke when a [[Slot]] is completed
   * @throws IllegalArgumentException If `callbacks` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If callbacks is null")
  def slotCompletionCallbacks(callbacks: immutable.Set[Try[PduResponse] => Unit]): T = {
    require(callbacks != null, "SlotCompletionCallbacks must not be null (use empty set instead)")
    this.slotCompletionCallbacks = callbacks
    this
  }

  /**
   * The [[ExecutionContext]] to run [[Slot]] completion callbacks on.
   *
   * @param exec The [[ExecutionContext]] to run [[Slot]] completion callbacks on
   * @throws IllegalArgumentException If exec is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If exec is null")
  def slotCompletionExecutionContext(exec: ExecutionContext): T = {
    require(exec != null, "ExecutionContext must not be null")
    this.slotCompletionExecutionContext = exec
    this
  }

  /**
   * Factory to use for creating new [[Window]]s.
   *
   * @param factory Factory to use for creating new [[Window]]s
   * @throws IllegalArgumentException If `factory` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If factory is null")
  def windowFactory(factory: WindowFactory[_ <: Window]): T = {
    require(factory != null, "WindowFactory must not be null")
    this.windowFactory = factory
    this
  }

  @throws[IllegalArgumentException]("If one of the required parameters is null or otherwise illegal")
  def buildNode(builderContext: BuilderContext): WindowConfig = {
    new WindowConfig(capacity,
      slotAcquisitionTimeout,
      slotExpirationTimeout,
      slotExpirationMonitoringInterval,
      slotExpirationMonitorExecutorFactory,
      slotCompletionCallbacks,
      slotCompletionExecutionContext,
      windowFactory,
      builderContext)
  }

}

/**
 * A builder for [[WindowConfig]]s.
 *
 * @param builderContext The [[BuilderContext]] to inject into new [[WindowConfig]] instances
 * @throws IllegalArgumentException If `builderContext` is `null`
 *
 * @see [[WindowConfigBuilderSupport]]
 */
@throws[IllegalArgumentException]("If builderContext is null")
final class WindowConfigBuilder(builderContext: BuilderContext)
  extends WindowConfigBuilderSupport[WindowConfigBuilder, WindowConfig] {

  /**
   * Build a new [[WindowConfig]] instance.
   *
   * @return A new [[WindowConfig]] instance, '''never''' `null`
   */
  override def build(): WindowConfig = buildNode(builderContext)
}

/**
 * Companion object for [[WindowConfigBuilder]].
 */
object WindowConfigBuilder {

  /**
   * Build a new [[WindowConfigBuilder]] instance, using the supplied [[BuilderContext]].
   *
   * @param builderContext The [[BuilderContext]] to inject into all new [[WindowConfig]] instances
   * @throws IllegalArgumentException If `builderContext` is `null`
   * @return A new [[WindowConfigBuilder]] instance
   */
  @throws[IllegalArgumentException]("If builderContext is null")
  def apply(builderContext: BuilderContext): WindowConfigBuilder = new WindowConfigBuilder(builderContext)
}
