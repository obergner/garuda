package io.garuda.windowing.support

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, Semaphore, TimeUnit}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.{PduRequest, PduResponse}
import io.garuda.common.metrics.ManagedMetricsSupport
import io.garuda.windowing._
import nl.grons.metrics.scala.{Counter, Meter, Timer}

import scala.collection.concurrent
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * A [[Window]] implementation that uses a [[Semaphore]] to ensure that that no more than [[Window.capacity]] [[PduRequest]]s
 * be allowed access at any given time.
 *
 * @param id This [[Window]]'s unique ID, typically the same as the ID of the `session` this `Window` is associated with
 * @param config This [[Window]]'s [[WindowConfig]]
 * @param metricRegistry [[MetricRegistry]] for all [[com.codahale.metrics.Metric]]s exposed by this [[Window]]
 * @throws IllegalArgumentException If `id` or `config` or `metricRegistry` is `null`
 *
 * @see [[Window]]<br/>
 *      [[WindowConfig]]
 */
@throws[IllegalArgumentException]("If id or config or metricRegistry is null")
class SemaphoreGuardedWindow(override val id: String,
                             config: WindowConfig,
                             protected[this] val metricRegistry: MetricRegistry)
  extends Window
  with ManagedMetricsSupport
  with Logging {
  require(id != null, "id must not be null")
  require(config != null, "WindowConfig must not be null")
  require(metricRegistry != null, "MetricRegistry must not be null")

  import io.garuda.windowing.support.SemaphoreGuardedWindow._

  private[this] implicit val slotCompletionExecutionContext: ExecutionContext = config.slotCompletionExecutionContext

  private[this] lazy val slotExpirationMonitorExecutor: ScheduledExecutorService = config.slotExpirationMonitorExecutor()

  private[this] val state: AtomicInteger = new AtomicInteger(State.Initial)

  private[this] val slotGuard: Semaphore = new Semaphore(capacity)

  private[this] val slots: concurrent.Map[Int, SlotImpl[_, _]] = concurrent.TrieMap[Int, SlotImpl[_, _]]()

  private[this] val expirationMonitorHandle: AtomicReference[ScheduledFuture[_]] = new
      AtomicReference[ScheduledFuture[_]]()

  private[this] val receivedPduRequestsPerSecond: Meter = metrics.meter("received-pdu-requests-per-second", id)

  private[this] val acceptedPduRequestsPerSecond: Meter = metrics.meter("accepted-pdu-requests-per-second", id)

  private[this] val slotAcquisitionsPerSecond: Timer = metrics.timer("slot-acquisitions-per-second", id)

  private[this] val processedPduRequests: Timer = metrics.timer("processed-pdu-requests", id)

  private[this] val slotAcquisitionTimeouts: Counter = metrics.counter("slot-acquisition-timeouts", id)

  metrics.gauge[Int]("capacity", id) {
    config.capacity
  }

  metrics.gauge[Int]("used-slots-count", id) {
    usedSlotsCount
  }

  override def defaultExpireTimeout: Duration = config.slotExpirationTimeout

  override def capacity: Int = config.capacity

  /**
   * Open this [[Window]], i.e. start threads/tasks etc. It is illegal for a client to use a [[Window]] before it has
   * been opened.
   *
   * @return This [[Window]]
   */
  override def open(): SemaphoreGuardedWindow = {
    if (!state.compareAndSet(State.Initial, State.Open)) {
      logger.warn(s"Attempt to open already opened/closing/closed ${this}")
      return this
    }
    assert(this.expirationMonitorHandle.compareAndSet(null, startExpirationMonitor()),
      "Illegal attempt to open a SemaphoreGuardedWindow that has already an ExpirationMonitor running")
    this
  }

  private[this] def startExpirationMonitor(): ScheduledFuture[_] = {
    val expirationMonitorHandle = slotExpirationMonitorExecutor.scheduleWithFixedDelay(ExpirationMonitor,
      config.slotExpirationMonitoringInterval.toMillis,
      config.slotExpirationMonitoringInterval.toMillis,
      TimeUnit.MILLISECONDS)
    logger.info(s"Started ${ExpirationMonitor}: [monitoring interval: ${config.slotExpirationMonitoringInterval.toMillis} ms|expiration timeout: ${config.slotExpirationTimeout.toMillis} ms]")
    expirationMonitorHandle
  }

  /**
   * Close this [[Window]], i.e. stop all internal threads/tasks, close all pending [[Slot]]s and unregister all its
   * [[com.codahale.metrics.Metric]]s. It is illegal to reuse a [[Window]] that has been stopped.
   *
   * @return This [[Window]]
   */
  override def close(): SemaphoreGuardedWindow = {
    if (!state.compareAndSet(State.Open, State.Closing)) {
      logger.warn(s"Attempt to close already closed/closing ${this}")
      return this
    }
    logger.info(s"Closing ${this} ...")
    slotGuard.drainPermits()
    stopExpirationMonitor()
    closePendingSlots()
    unregisterAllMetrics()
    logger.info(s"${this} has been closed")
    this
  }

  private[this] def stopExpirationMonitor(): Unit = {
    logger.info(s"Stopping ${ExpirationMonitor} ...")
    expirationMonitorHandle.get().cancel(true)
    slotExpirationMonitorExecutor.shutdown()
    if (!slotExpirationMonitorExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
      logger.warn(s"${ExpirationMonitor} did not stop after [100] ms - will be terminated HARD")
      slotExpirationMonitorExecutor.shutdownNow()
    }
    logger.info(s"${ExpirationMonitor} has been stopped")
  }

  private[this] def closePendingSlots(): Unit = {
    logger.info(s"Closing pending slots ...")
    var closedSlots = 0
    slots.values.foreach {
      slot => if (slot.tryClose()) closedSlots += 1
    }
    logger.info(s"Closed [${closedSlots}] pending slots")
  }

  /**
   *
   * @param request
   * @tparam P
   * @tparam R
   * @return
   */
  override def tryAcquireSlot[P <: PduResponse, R <: PduRequest[P]](request: R): Try[Slot[P, R]] = {
    require(request.sequenceNumber.isDefined, "A PduRequest to be stored in a window store MUST have a sequence number assigned")
    Try(doTryAcquireSlot(request))
  }

  private def doTryAcquireSlot[R <: PduRequest[P], P <: PduResponse](request: R): Slot[P, R] = {
    logger.debug(s"Trying to acquire a slot for [${request}] using an acquisition timeout of " +
      s"[${config.slotAcquisitionTimeout.toMillis}] ms and an expiration timeout of [${config.slotExpirationTimeout.toMillis}] ms")
    receivedPduRequestsPerSecond.mark
    waitForFreeSlot(request, config.slotAcquisitionTimeout)
    val newSlot = new SlotImpl[P, R](request, config.slotExpirationTimeout)
    slots.putIfAbsent(newSlot.key, newSlot) match {
      case Some(oldPduRequest) => throw new DuplicateSequenceNumberException(request)
      case _ => logger.debug(s"Acquired slot for [${request}]")
    }
    acceptedPduRequestsPerSecond.mark
    val usedSlotsCount = slots.size
    assert(usedSlotsCount <= capacity, s"This Window MUST NOT store more than ${capacity} requests, but number of used slots is [${usedSlotsCount}]")
    closePendingSlotsIfClosingOrClosed()
    newSlot
  }

  private[this] def waitForFreeSlot(request: PduRequest[_], acquireTimeout: Duration): Unit = {
    slotAcquisitionsPerSecond.time {
      if (!slotGuard.tryAcquire(acquireTimeout.toMillis, TimeUnit.MILLISECONDS)) {
        slotAcquisitionTimeouts.inc()
        throw new SlotAcquisitionTimedOutException(request, acquireTimeout)
      }
    }
  }

  private[this] def closePendingSlotsIfClosingOrClosed(): Unit = {
    if (state.get() > State.Open) {
      closePendingSlots()
    }
  }

  override def usedSlotsCount: Int = slots.size

  override def toString: String = s"SemaphoreGuardedWindow[id:${id}]"

  private[this] class SlotImpl[P <: PduResponse, R <: PduRequest[P]](val request: R, val expireTimeout: Duration)
    extends Slot[P, R] with Logging {
    require(request.sequenceNumber.isDefined, "A PduRequest to be stored in a window store MUST have a sequence number assigned")

    import scala.concurrent.promise

    val key: Int = request.sequenceNumber.get

    val acquisitionTimeNanos: Long = System.nanoTime()

    val expirationTimeNanos: Long = acquisitionTimeNanos + expireTimeout.toNanos

    private[this] val responsePromise: Promise[P] = promise[P]

    /*
     * Release this slot upon completion
     */
    responsePromise.future.onComplete {
      case _ =>
        assert(slots.remove(key, this), "There must be only one place where a slot is cleared")
        if (state.get() == State.Open) {
          slotGuard.release()
          logger.debug(s"Released ${this}")
        }
    }

    /*
     * Update processing time statistics
     */
    responsePromise.future.onComplete {
      case _ => processedPduRequests.update(System.nanoTime() - acquisitionTimeNanos, TimeUnit.NANOSECONDS)
    }

    config.slotCompletionCallbacks.foreach {
      slotCompletionCb => responsePromise.future.onComplete(slotCompletionCb)
    }

    def release(response: P): Try[Unit] = {
      if (responsePromise.trySuccess(response)) {
        Success(())
      } else {
        /* At this point we KNOW that future.value will NEVER be None
         *
         */
        responsePromise.future.value.get match {
          case Success(resp) => Failure(new
              IllegalStateException(s"Duplicate attempt to release Slot: ${this} has already been released with ${resp}").fillInStackTrace())
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def release(error: Throwable): Try[Unit] = {
      if (responsePromise.tryFailure(error)) {
        Success(())
      } else {
        /* At this point we KNOW that future.value will NEVER be None
         *
         */
        responsePromise.future.value.get match {
          case Success(response) => Failure(new
              IllegalStateException(s"Duplicate attempt to release Slot: ${this} has already been released with ${response}").fillInStackTrace())
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def releaseFuture: Future[P] = responsePromise.future

    private[this] def isExpired: Boolean = System.nanoTime() > acquisitionTimeNanos + expireTimeout.toNanos

    private[support] def checkExpiredNow(): Boolean = if (isExpired) {
      responsePromise.tryFailure(new SlotExpiredException(request, expireTimeout).fillInStackTrace())
    } else {
      false
    }

    private[support] def tryClose(): Boolean = {
      import io.garuda.windowing.WindowClosingOrClosedException._
      responsePromise.tryFailure(windowClosedWhileWaitingForResponse(request).fillInStackTrace())
    }

    override def toString: String = s"${getClass.getSimpleName}(key:${key},request:${request},acquisitionTimeNanos:${acquisitionTimeNanos})"
  }

  private[this] object ExpirationMonitor extends Runnable with Logging {

    override def run(): Unit = {
      val currentThreadName = Thread.currentThread().getName
      try {
        Thread.currentThread().setName(currentThreadName + "@" + id)
        var expiredSlots = 0
        slots.values.foreach {
          slot => if (slot.checkExpiredNow()) expiredSlots += 1
        }
        logger.info(s"Expired [${expiredSlots}] slots")
      } catch {
        case e: InterruptedException =>
          // Restore 'interrupted' flag
          Thread.currentThread().interrupt()
          logger.info(s"Got interrupted while trying to expire overdue slots - assume that this Window is closing")
        case t: Throwable =>
          logger.error(s"Caught unexpected exception while trying to expire overdue slots: ${t.getMessage}", t)
          throw t
      } finally {
        if (currentThreadName != null) {
          Thread.currentThread().setName(currentThreadName)
        }
      }
    }

    override def toString(): String = s"ExpirationMonitor[window-id:${id}]"
  }

}

object SemaphoreGuardedWindow {

  object State {

    val Initial: Int = 0

    val Open: Int = 1

    val Closing: Int = 2

    val Closed: Int = 3
  }

}
