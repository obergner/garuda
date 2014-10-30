package io.garuda.windowing.support

import java.util.concurrent.{CountDownLatch, Executors, ScheduledExecutorService, TimeUnit}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.{EnquireLink, EnquireLinkResponse, PduResponse}
import io.garuda.common.builder.BuilderContext
import io.garuda.common.resources.ResourcesRegistry
import io.garuda.windowing.{DuplicateSequenceNumberException, SlotAcquisitionTimedOutException, SlotExpiredException, WindowClosingOrClosedException, WindowConfig, WindowConfigBuilder}
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Specification, Tags}
import org.specs2.specification.AroundOutside
import org.specs2.time.NoTimeConversions

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Created by obergner on 12.04.14.
 */
class SemaphoreGuardedWindowSpec extends Specification with Tags with NoTimeConversions with Logging {

  logger.info(s"Initializing logging subsystem for test ${this}")

  def builderContext(name: String): BuilderContext = {
    val metricRegistry = new MetricRegistry
    val resourcesRegistry = new ResourcesRegistry(name, 10 milliseconds, metricRegistry)
    BuilderContext(metricRegistry, resourcesRegistry)
  }

  def semaphoreGuardedWindow(capacity: Int,
                             acquisitionTimeout: Duration,
                             defaultExpireTimeout: Duration,
                             slotCompletionCallbacks: immutable.Set[Try[PduResponse] => Unit],
                             slotExpirationMonitorExecutor: ScheduledExecutorService,
                             slotExpirationMonitoringInterval: Duration,
                             executionContext: ExecutionContext,
                             builderContext: BuilderContext): SemaphoreGuardedWindow = {
    val windowConfig = WindowConfigBuilder(builderContext)
      .capacity(capacity)
      .slotAcquisitionTimeout(acquisitionTimeout)
      .slotExpirationTimeout(defaultExpireTimeout)
      .slotCompletionCallbacks(slotCompletionCallbacks)
      .slotExpirationMonitorExecutorFactory(Function.const[ScheduledExecutorService, WindowConfig](slotExpirationMonitorExecutor) _)
      .slotExpirationMonitoringInterval(slotExpirationMonitoringInterval)
      .slotCompletionExecutionContext(executionContext)
      .build()
    new SemaphoreGuardedWindow("SemaphoreGuardedWindowSpec", windowConfig, new MetricRegistry)
  }

  case class window(capacity: Int,
                    acquisitionTimeout: Duration,
                    defaultExpireTimeout: Duration,
                    slotCompletionCallbacks: immutable.Set[Try[PduResponse] => Unit],
                    slotExpirationMonitorExecutor: ScheduledExecutorService,
                    slotExpirationMonitoringInterval: Duration,
                    executionContext: ExecutionContext) extends AroundOutside[SemaphoreGuardedWindow] {

    private[this] val builderCtx = builderContext("SemaphoreGuardedWindowSpec")

    private[this] lazy val objectUnderTest = semaphoreGuardedWindow(capacity,
      acquisitionTimeout,
      defaultExpireTimeout,
      slotCompletionCallbacks,
      slotExpirationMonitorExecutor,
      slotExpirationMonitoringInterval,
      executionContext,
      this.builderCtx)

    override def outside: SemaphoreGuardedWindow = objectUnderTest

    override def around[R](a: => R)(implicit evidence$3: AsResult[R]): Result = {
      try {
        this.builderCtx.open()
        AsResult(a)
      } finally {
        outside.close()
        this.builderCtx.close()
      }
    }
  }

  "SemaphoreGuardedWindow constructor" should {

    "reject 0 capacity" in {
      semaphoreGuardedWindow(0,
        1 second,
        1 second,
        immutable.Set.empty[Try[PduResponse] => Unit],
        Executors.newSingleThreadScheduledExecutor(),
        100 seconds,
        ExecutionContext.Implicits.global,
        builderContext("reject 0 capacity")) must throwAn[IllegalArgumentException]
    } tag "unit"


    "reject null slotAcquisitionTimeout" in {
      semaphoreGuardedWindow(10,
        null,
        1 second,
        immutable.Set.empty[Try[PduResponse] => Unit],
        Executors.newSingleThreadScheduledExecutor(),
        100 seconds,
        ExecutionContext.Implicits.global,
        builderContext("reject null slotAcquisitionTimeout")) must throwAn[IllegalArgumentException]
    } tag "unit"

    "reject null defaultExpirationTimeout" in {
      semaphoreGuardedWindow(10,
        1 second,
        null,
        immutable.Set.empty[Try[PduResponse] => Unit],
        Executors.newSingleThreadScheduledExecutor(),
        100 seconds,
        ExecutionContext.Implicits.global,
        builderContext("reject null defaultExpirationTimeout")) must throwAn[IllegalArgumentException]
    } tag "unit"

    "reject null expirationMonitoringInterval" in {
      semaphoreGuardedWindow(10,
        1 second,
        1 second,
        immutable.Set.empty[Try[PduResponse] => Unit],
        Executors.newSingleThreadScheduledExecutor(),
        null,
        ExecutionContext.Implicits.global,
        builderContext("reject null expirationMonitoringInterval")) must throwAn[IllegalArgumentException]
    } tag "unit"

    "reject null slotCompletionExecutionContext" in {
      semaphoreGuardedWindow(10,
        1 second,
        1 second,
        immutable.Set.empty[Try[PduResponse] => Unit],
        Executors.newSingleThreadScheduledExecutor(),
        100 seconds,
        null,
        builderContext("reject null slotCompletionExecutionContext")) must throwAn[IllegalArgumentException]
    } tag "unit"
  }

  "tryAcquireSlot" should {

    "reject PduRequest without SequenceNumber" in window(10,
      1 second,
      1 second,
      immutable.Set.empty[Try[PduResponse] => Unit],
      Executors.newSingleThreadScheduledExecutor(),
      100 seconds,
      ExecutionContext.Implicits.global) {
      (objectUnderTest: SemaphoreGuardedWindow) => {
        val enquireLinkWithoutSeqNo = EnquireLink(None)

        objectUnderTest.open()
        objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](enquireLinkWithoutSeqNo) must throwAn[IllegalArgumentException]
      }
    } tag "integration"

    "immediately accept up to capacity requests" in window(10,
      100 seconds,
      100 seconds,
      immutable.Set.empty[Try[PduResponse] => Unit],
      Executors.newSingleThreadScheduledExecutor(),
      100 seconds,
      ExecutionContext.Implicits.global) {
      (objectUnderTest: SemaphoreGuardedWindow) => {
        objectUnderTest.open()

        for (idx <- 1 to objectUnderTest.capacity) {
          objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(idx))
        }

        objectUnderTest.usedSlotsCount mustEqual objectUnderTest.capacity
      }
    } tag "integration"

    "reject first request after capacity requests have been accepted" in window(10,
      10 milliseconds,
      100 seconds,
      immutable.Set.empty[Try[PduResponse] => Unit],
      Executors.newSingleThreadScheduledExecutor(),
      100 seconds,
      ExecutionContext.Implicits.global) {
      (objectUnderTest: SemaphoreGuardedWindow) => {
        objectUnderTest.open()

        for (idx <- 1 to objectUnderTest.capacity) {
          objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(idx))
        }

        objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(objectUnderTest.capacity + 1)).get must throwA[SlotAcquisitionTimedOutException]
      }
    } tag "integration"

    "reject request with duplicate sequence number" in window(10,
      10 milliseconds,
      100 seconds,
      immutable.Set.empty[Try[PduResponse] => Unit],
      Executors.newSingleThreadScheduledExecutor(),
      100 seconds,
      ExecutionContext.Implicits.global) {
      (objectUnderTest: SemaphoreGuardedWindow) => {
        objectUnderTest.open()

        for (idx <- 1 to objectUnderTest.capacity - 1) {
          objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(idx))
        }

        objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(objectUnderTest.capacity - 1)).get must throwA[DuplicateSequenceNumberException]
      }
    } tag "integration"

    "free a slot if a response promise is fulfilled" in window(10,
      1 second,
      100 seconds,
      immutable.Set.empty[Try[PduResponse] => Unit],
      Executors.newSingleThreadScheduledExecutor(),
      100 seconds,
      ExecutionContext.Implicits.global) {
      (objectUnderTest: SemaphoreGuardedWindow) => {
        objectUnderTest.open()

        objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(1)) match {
          case Success(slot) => slot.release(EnquireLinkResponse(0, 1))
          case Failure(ex) => throw ex
        }

        objectUnderTest.availableSlotsCount mustEqual objectUnderTest.capacity
      }
    } tag "integration"
  }

  "close" should {

    "complete each slot with a WindowClosingOrClosedException" in window(10,
      1 second,
      100 seconds,
      immutable.Set.empty[Try[PduResponse] => Unit],
      Executors.newSingleThreadScheduledExecutor(),
      100 seconds,
      ExecutionContext.Implicits.global) {
      (objectUnderTest: SemaphoreGuardedWindow) => {
        objectUnderTest.open()

        val allSlotsHaveBeenClosed = new CountDownLatch(objectUnderTest.capacity)
        for (idx <- 1 to objectUnderTest.capacity) {
          objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(idx)) match {
            case Success(slot) => slot.releaseFuture.onFailure {
              case c: WindowClosingOrClosedException => allSlotsHaveBeenClosed.countDown()
            }
            case Failure(ex) => throw ex
          }
        }
        objectUnderTest.close()

        allSlotsHaveBeenClosed.await(100, TimeUnit.MILLISECONDS) must beTrue
      }
    } tag "integration"
  }

  "ExpirationMonitor" should {
    val capacity = 10
    val allSlotsHaveBeenExpired = new CountDownLatch(capacity)
    val expirationListener = (result: Try[PduResponse]) => {
      result match {
        case Failure(t) if t.isInstanceOf[SlotExpiredException] => allSlotsHaveBeenExpired.countDown()
      }
    }

    "expire all overdue slots with a SlotExpiredException" in window(capacity,
      1 second,
      10 milliseconds,
      immutable.Set[Try[PduResponse] => Unit](expirationListener),
      Executors.newSingleThreadScheduledExecutor(),
      100 milliseconds,
      ExecutionContext.Implicits.global) {
      (objectUnderTest: SemaphoreGuardedWindow) => {
        objectUnderTest.open()

        for (idx <- 1 to capacity) {
          objectUnderTest.tryAcquireSlot[EnquireLinkResponse, EnquireLink](EnquireLink(idx))
        }

        allSlotsHaveBeenExpired.await(250, TimeUnit.MILLISECONDS) must beTrue
      }
    } tag "integration"
  }
}
