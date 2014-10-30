package io.garuda.common.resources

import java.lang.ref.{ReferenceQueue, WeakReference}
import java.util
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ConcurrentHashMap, ExecutorService, TimeUnit}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.common.metrics.ManagedMetricsSupport
import io.netty.util.Timer

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration

/**
 * A registry for resources. In this context, a `resource` is a component that needs to be closed in some way to release
 * "system resources" like e.g. threads, file handles, memory it holds on to. A [[ResourcesRegistry]] keeps references
 * to all such resources in order to ensure that they will be properly closed upon application shutdown.
 *
 * @param name This `ResourcesRegistry`'s unique name
 * @param shutdownTimeout Timeout for attempts to shut down resources managed by this `ResourcesRegistry`
 * @param metricRegistry [[MetricRegistry]] for exposing metrics
 * @throws IllegalArgumentException If one of the parameters is `null`
 */
@throws[IllegalArgumentException]("If one of the parameters is null")
class ResourcesRegistry(val name: String, val shutdownTimeout: Duration, protected[this] val metricRegistry: MetricRegistry)
  extends ManagedMetricsSupport with Logging {
  require(name != null, "Argument 'name' must not be null")
  require(shutdownTimeout != null, "Argument 'shutdownTimeout' must not be null")
  require(metricRegistry != null, "Argument 'metricRegistry' must not be null")

  import io.garuda.common.resources.ResourcesRegistry.State

  private[this] val executorServicesRefQueue: ReferenceQueue[ExecutorService] = new ReferenceQueue[ExecutorService]

  private[this] val executorServices: java.util.Set[WeakExecutorServiceReference] =
    java.util.Collections.newSetFromMap[WeakExecutorServiceReference](new
        ConcurrentHashMap[WeakExecutorServiceReference, java.lang.Boolean]())

  private[this] val executorServicesCleanup: ExecutorServicesCleanupThread = new ExecutorServicesCleanupThread

  private[this] val timersRefQueue: ReferenceQueue[Timer] = new ReferenceQueue[Timer]

  private[this] val timers: java.util.Set[WeakTimerReference] =
    java.util.Collections.newSetFromMap[WeakTimerReference](new
        ConcurrentHashMap[WeakTimerReference, java.lang.Boolean]())

  private[this] val timersCleanup: TimersCleanupThread = new TimersCleanupThread

  private[this] val currentState: AtomicInteger = new AtomicInteger(State.Initial)

  metrics.gauge[Int]("executor-services-count", name) {
    executorServices.size()
  }

  metrics.gauge[Int]("timers-count", name) {
    timers.size()
  }

  metrics.gauge[String]("executor-services-cleanup-thread-state", name) {
    executorServicesCleanup.getState.name()
  }

  metrics.gauge[String]("timers-cleanup-thread-state", name) {
    timersCleanup.getState.name()
  }

  /**
   * Start this [[ResourcesRegistry]], i.e. start internal cleanup threads. Does nothing when this [[ResourcesRegistry]]
   * is already started.
   */
  def start(): Unit = {
    if (!this.currentState.compareAndSet(State.Initial, State.Started)) {
      logger.warn(s"Ignoring attempt to start already started/stopped ${this}")
      return
    }
    logger.info(s"Starting ${this} ...")
    val start = System.currentTimeMillis()
    startExecutorServicesCleanup()
    startTimersCleanup()
    logger.info(s"${this} started in [${System.currentTimeMillis() - start}] ms")
  }

  private[this] def startExecutorServicesCleanup(): Unit = {
    if (!this.executorServicesCleanup.isAlive()) {
      this.executorServicesCleanup.start()
    }
  }

  private[this] def startTimersCleanup(): Unit = {
    if (!this.timersCleanup.isAlive) {
      this.timersCleanup.start()
    }
  }

  /**
   * Register supplied [[ExecutorService]] with this [[ResourcesRegistry]].
   *
   * @param executorService The [[ExecutorService]] to registerTimer, must not be `null`
   * @throws IllegalArgumentException If executorService is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If executorService is null")
  def registerExecutorService(executorService: ExecutorService): this.type = {
    require(executorService != null, "Argument 'executorService' must not be null")
    this.executorServices += new WeakExecutorServiceReference(executorService, this.executorServicesRefQueue)
    this
  }

  /**
   * Take the supplied `executorServiceFactory` and return a "wrapper" function that uses `executorServiceFactory` to
   * produce an [[ExecutorService]], registers that [[ExecutorService]] with this [[ResourcesRegistry]] and returns it.
   *
   * @param executorServiceFactory The factory to "wrap"
   * @tparam P
   * @return A wrapper function
   */
  def registerExecutorService[R <: ExecutorService, P](executorServiceFactory: P => R): P => R = {
    p: P => {
      val result = executorServiceFactory(p)
      registerExecutorService(result)
      result
    }
  }

  /**
   * Take the supplied `executorServiceFactory` and return a "wrapper" function that uses `executorServiceFactory` to
   * produce an [[ExecutorService]], registers that [[ExecutorService]] with this [[ResourcesRegistry]] and returns it.
   *
   * @param executorServiceFactory The factory to "wrap"
   * @tparam P
   * @tparam Q
   * @return A wrapper function
   */
  def registerExecutorService[R <: ExecutorService, P, Q](executorServiceFactory: (P, Q) => R): (P, Q) => R = {
    (p: P, q: Q) => {
      val result = executorServiceFactory(p, q)
      registerExecutorService(result)
      result
    }
  }

  /**
   * Register supplied [[Timer]] with this [[ResourcesRegistry]].
   *
   * @param timer The [[Timer]] to registerTimer, must not be `null`
   * @throws IllegalArgumentException If `timer` is `null`
   * @return this
   */
  @throws[IllegalArgumentException]("If timer is null")
  def registerTimer(timer: Timer): this.type = {
    require(timer != null, "Argument 'timer' must not be null")
    this.timers += new WeakTimerReference(timer, this.timersRefQueue)
    this
  }

  /**
   * Take the supplied `timerFactory` and return a "wrapper" function that uses `timerFactory` to produce a [[Timer]],
   * registers that [[Timer]] with this [[ResourcesRegistry]] and returns it.
   *
   * @param timerFactory The factory to "wrap"
   * @tparam P
   * @return A wrapper function
   */
  def registerTimer[P](timerFactory: P => Timer): P => Timer = {
    p: P => {
      val result = timerFactory(p)
      registerTimer(result)
      result
    }
  }

  /**
   * Take the supplied `timerFactory` and return a "wrapper" function that uses `timerFactory` to produce a [[Timer]],
   * registers that [[Timer]] with this [[ResourcesRegistry]] and returns it.
   *
   * @param timerFactory The factory to "wrap"
   * @tparam P
   * @tparam Q
   * @return A wrapper function
   */
  def registerTimer[P, Q](timerFactory: (P, Q) => Timer): (P, Q) => Timer = {
    (p: P, q: Q) => {
      val result = timerFactory(p, q)
      registerTimer(result)
      result
    }
  }

  /**
   * Stop this [[ResourcesRegistry]], i.e. shutdown/stop all registered [[Timer]]s and [[ExecutorService]]s, stop all
   * internal cleanup threads. Does nothing if this [[ResourcesRegistry]] is not started.
   */
  def stop(): Unit = {
    if (!this.currentState.compareAndSet(State.Started, State.Stopped)) {
      logger.warn(s"Ignoring attempt to stop not yet started/already stopped ${this}")
      return
    }
    logger.info(s"Stopping ${this} ...")
    shutdownPendingExecutorServices()
    stopExecutorServicesCleanup()
    stopPendingTimers()
    stopTimersCleanup()
    unregisterAllMetrics()
    logger.info(s"${this} stopped")
  }

  private[this] def shutdownPendingExecutorServices(): Unit = {
    logger.info(s"Shutting down ${this.executorServices.size} pending ExecutorServices ...")
    val start = System.currentTimeMillis()
    this.executorServices.foreach {
      _.shutdown(shutdownTimeout.toMillis)
    }
    logger.info(s"Shut down all pending ExecutorServices in [${System.currentTimeMillis() - start}] ms")
  }

  private[this] def stopExecutorServicesCleanup(): Unit = {
    this.executorServicesCleanup.interrupt()
  }

  private[this] def stopPendingTimers(): Unit = {
    logger.info(s"Shutting down ${this.timers.size} pending Timers ...")
    val start = System.currentTimeMillis()
    this.timers.foreach {
      _.shutdown(shutdownTimeout.toMillis)
    }
    logger.info(s"Shut down all pending Timers in [${System.currentTimeMillis() - start}] ms")
  }

  private[this] def stopTimersCleanup(): Unit = {
    this.timersCleanup.interrupt()
  }

  override def toString: String = s"ResourcesRegistry[${name}]"

  /**
   *
   * @param executorService
   * @param referenceQueue
   */
  private class WeakExecutorServiceReference(executorService: ExecutorService, referenceQueue: ReferenceQueue[ExecutorService])
    extends WeakReference[ExecutorService](executorService, referenceQueue) {
    require(executorService != null, "ExecutorService must not be null")

    val identity: Int = System.identityHashCode(executorService)

    def shutdown(timeoutMillis: Long): Unit = {
      executorServices.remove(this)
      val exec: ExecutorService = get()
      if (exec != null) {
        doShutdown(exec, timeoutMillis)
      }
    }

    private[this] def doShutdown(executorService: ExecutorService, timeoutMillis: Long): Unit = {
      if (executorService.isShutdown) return
      executorService.shutdown()
      logger.info(s"Shutdown of ${executorService} initiated - will wait for ${timeoutMillis} ms until shutdown is completed")
      if (!executorService.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
        val pendingTasks: util.List[Runnable] = executorService.shutdownNow()
        logger.warn(s"${executorService} did not cleanly shutdown after ${timeoutMillis} ms - ${pendingTasks.size} are/were still running")
      } else {
        logger.info(s"${executorService} was cleanly shutdown")
      }
    }

    override def hashCode(): Int = identity

    override def equals(obj: scala.Any): Boolean = {
      if (obj == null) return false
      if (!obj.isInstanceOf[WeakExecutorServiceReference]) return false
      identity == obj.asInstanceOf[WeakExecutorServiceReference].identity
    }
  }

  /**
   *
   * @param timer
   * @param referenceQueue
   */
  private class WeakTimerReference(timer: Timer, referenceQueue: ReferenceQueue[Timer])
    extends WeakReference[Timer](timer, referenceQueue) {
    require(timer != null, "Timer must not be null")

    val identity: Int = System.identityHashCode(timer)

    def shutdown(timeoutMillis: Long): Unit = {
      timers.remove(this)
      val timer: Timer = get()
      if (timer != null) {
        doShutdown(timer, timeoutMillis)
      }
    }

    private[this] def doShutdown(timer: Timer, timeoutMillis: Long): Unit = {
      val pendingTimeouts = timer.stop()
      pendingTimeouts.foreach { timeout => timeout.cancel()}
      logger.info(s"${timer} has been stopped")
    }

    override def hashCode(): Int = identity

    override def equals(obj: scala.Any): Boolean = {
      if (obj == null) return false
      if (!obj.isInstanceOf[WeakTimerReference]) return false
      identity == obj.asInstanceOf[WeakTimerReference].identity
    }
  }

  /**
   *
   */
  private class ExecutorServicesCleanupThread extends Thread("executor-services-cleanup#" + name) {

    override def run(): Unit = {
      logger.info(s"ExecutorServicesCleanupThread starts")
      while (!Thread.currentThread().isInterrupted) {
        try {
          val executorServiceRef: WeakExecutorServiceReference = executorServicesRefQueue.remove().asInstanceOf[WeakExecutorServiceReference]
          logger.warn(s"ExecutorService ${executorServiceRef.get()} has not been properly shutdown prior to being garbage collected " +
            s"- make sure to properly close all expensive resources")
          executorServiceRef.shutdown(shutdownTimeout.toMillis)
        }
        catch {
          case ie: InterruptedException =>
            // Restore interrupted flag
            Thread.currentThread().interrupt()
        }
      }
      logger.info(s"ExecutorServicesCleanupThread exits")
    }
  }

  /**
   *
   */
  private class TimersCleanupThread extends Thread("timers-cleanup#" + name) {

    override def run(): Unit = {
      logger.info(s"TimersCleanupThread starts")
      while (!Thread.currentThread().isInterrupted) {
        try {
          val timerRef: WeakTimerReference = timersRefQueue.remove().asInstanceOf[WeakTimerReference]
          logger.warn(s"Timer ${timerRef.get()} has not been properly stopped prior to being garbage collected " +
            s"- make sure to properly close all expensive resources")
          timerRef.shutdown(shutdownTimeout.toMillis)
        }
        catch {
          case ie: InterruptedException =>
            // Restore interrupted flag
            Thread.currentThread().interrupt()
        }
      }
      logger.info(s"TimersCleanupThread exits")
    }
  }

}

/**
 * Companion object for [[ResourcesRegistry]].
 */
object ResourcesRegistry {

  /**
   * Simple state model for [[ResourcesRegistry]].
   */
  object State {

    /**
     * The [[ResourcesRegistry]] has been create but not yet started.
     */
    val Initial: Int = 0

    /**
     * The [[ResourcesRegistry]] has been started, i.e. is currently active.
     */
    val Started: Int = 1

    /**
     * The [[ResourcesRegistry]] has been stopped and may not be reused.
     */
    val Stopped: Int = 2
  }

  /**
   * Create a new [[ResourcesRegistry]] instance.
   *
   * @param name The new [[ResourcesRegistry]]'s `name`
   * @param shutdownTimeout The new [[ResourcesRegistry]]'s `shutdownTimeout`
   * @param metricRegistry The new [[ResourcesRegistry]]'s [[MetricRegistry]]
   * @return A new [[ResourcesRegistry]] instance, '''never''' `null`
   */
  def apply(name: String, shutdownTimeout: Duration, metricRegistry: MetricRegistry): ResourcesRegistry =
    new ResourcesRegistry(name, shutdownTimeout, metricRegistry)
}
