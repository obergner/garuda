package io.garuda.windowing.support

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.concurrent.{CountDownLatch, Executors, ScheduledExecutorService, TimeUnit}

import com.codahale.metrics.{JmxReporter, MetricRegistry}
import com.typesafe.scalalogging.slf4j.Logging
import io.garuda.codec.pdu.{EnquireLink, EnquireLinkResponse, PduResponse}
import io.garuda.common.builder.BuilderContext
import io.garuda.common.resources.ResourcesRegistry
import io.garuda.windowing._
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Around, Specification, Tags}
import org.specs2.time.NoTimeConversions

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

/**
 * Created by obergner on 17.04.14.
 */
class SemaphoreGuardedWindowConcurrencySpec extends Specification with Tags with NoTimeConversions with Logging {

  logger.info(s"Initialize logging subsystem for ${this}")

  val capacity = 50
  val defaultAcquisitionTimeout = 20 milliseconds
  val defaultExpirationTimeout = 200 milliseconds
  val expirationMonitorInterval = 100 milliseconds
  val numberOfRequests = 100000
  val numberOfRequestThreads = 100
  val numberOfResponseThreads = numberOfRequestThreads
  val mostRequestsHaveBeenPosted = new CountDownLatch(numberOfRequests * 95 / 100)
  val allRequestsHaveBeenAccountedFor = new CountDownLatch(numberOfRequests)
  val numberOfAcquireTimedOutRequests = new AtomicInteger(0)
  val numberOfExpiredRequests = new AtomicInteger(0)
  val numberOfClosedRequests = new AtomicInteger(0)
  val numberOfRequestsCompletedWithAResponse = new AtomicInteger(0)
  val allRequestThreadsHaveBeenStarted = new CountDownLatch(numberOfRequestThreads + 1)
  val nextSequenceNumber = new AtomicInteger(0)

  val requestExecutor = Executors.newFixedThreadPool(numberOfRequestThreads)
  val responseExecutor = Executors.newFixedThreadPool(numberOfResponseThreads)

  val registry: MetricRegistry = new MetricRegistry
  val jmxReporter = JmxReporter.forRegistry(registry).build()

  val objectUnderTestHolder: AtomicReference[SemaphoreGuardedWindow] = new AtomicReference[SemaphoreGuardedWindow]()

  val builderContext: BuilderContext = {
    val resourcesRegistry = new ResourcesRegistry(getClass.getSimpleName, 10 milliseconds, registry)
    BuilderContext(registry, resourcesRegistry)
  }

  object test_context extends Around {
    override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
      try {
        builderContext.resourcesRegistry.start()
        jmxReporter.start()
        AsResult(t)
      } finally {
        if (objectUnderTestHolder.get() != null) {
          objectUnderTestHolder.get().close()
          objectUnderTestHolder.set(null)
        }
        jmxReporter.stop()
        requestExecutor.shutdown()
        if (!requestExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          requestExecutor.shutdownNow()
        }
        responseExecutor.shutdown()
        if (!responseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          responseExecutor.shutdownNow()
        }
        builderContext.resourcesRegistry.stop()
      }
    }
  }

  "SemaphoreGuardedWindow" should {

    "account for all processed requests" in test_context {

      val slotCompletionMonitor = (result: Try[PduResponse]) => {
        result match {
          case Failure(t: SlotExpiredException) => numberOfExpiredRequests.incrementAndGet()
          case Failure(t: WindowClosingOrClosedException) => numberOfClosedRequests.incrementAndGet()
          case Failure(t: Throwable) => throw t
          case Success(response) => numberOfRequestsCompletedWithAResponse.incrementAndGet()
        }
        allRequestsHaveBeenAccountedFor.countDown()
      }

      val windowConfig = WindowConfigBuilder(builderContext)
        .capacity(capacity)
        .slotAcquisitionTimeout(defaultAcquisitionTimeout)
        .slotExpirationTimeout(defaultExpirationTimeout)
        .slotCompletionCallbacks(immutable.Set[Try[PduResponse] => Unit](slotCompletionMonitor))
        .slotExpirationMonitorExecutorFactory(Function.const[ScheduledExecutorService, WindowConfig](Executors.newSingleThreadScheduledExecutor()) _)
        .slotExpirationMonitoringInterval(expirationMonitorInterval)
        .slotCompletionExecutionContext(ExecutionContext.Implicits.global)
        .build()

      val objectUnderTest = new
          SemaphoreGuardedWindow("SemaphoreGuardedWindowConcurrencySpec", windowConfig, registry).open()
      objectUnderTestHolder.set(objectUnderTest)

      for (i <- 1 to numberOfRequests) {
        requestExecutor.submit(new AcquireSlotForRequest(objectUnderTest))
      }
      allRequestThreadsHaveBeenStarted.countDown()

      // Stop SemaphoreGuardedWindow as soon as all requests have been posted
      mostRequestsHaveBeenPosted.await(90, TimeUnit.SECONDS) must beTrue

      allRequestsHaveBeenAccountedFor.await(30, TimeUnit.SECONDS) must beTrue

      logger.info(s"Total number of requests:                           ${numberOfRequests}")
      logger.info(s"Number of requests with acquisition timeout:        ${numberOfAcquireTimedOutRequests.get()}")
      logger.info(s"Number of closed requests:                          ${numberOfClosedRequests.get()}")
      logger.info(s"Number of expired requests:                         ${numberOfExpiredRequests.get()}")
      logger.info(s"Number of requests completed with a response:       ${numberOfRequestsCompletedWithAResponse.get()}")
      (numberOfAcquireTimedOutRequests.get() + numberOfClosedRequests.get() + numberOfExpiredRequests.get() + numberOfRequestsCompletedWithAResponse.get()) mustEqual numberOfRequests
    } tag "multithreaded"
  }

  private[this] class AcquireSlotForRequest(window: SemaphoreGuardedWindow) extends Runnable with Logging {

    override def run(): Unit = {
      allRequestThreadsHaveBeenStarted.countDown()
      allRequestThreadsHaveBeenStarted.await()
      val request: EnquireLink = EnquireLink(nextSequenceNumber.incrementAndGet())
      try {
        val randomSleepTime = new Random().nextInt(10) + 2
        Thread.sleep(randomSleepTime)
        val triedSlot = window.tryAcquireSlot[EnquireLinkResponse, EnquireLink](request)
        triedSlot match {
          case Success(slot) => responseExecutor.execute(new CompleteRequestWithResponse(slot))
          case Failure(satoe: SlotAcquisitionTimedOutException) =>
            logger.info(s"Acquiring a slot for ${request} timed out after [20] ms")
            numberOfAcquireTimedOutRequests.incrementAndGet()
            allRequestsHaveBeenAccountedFor.countDown()
          case Failure(t) => logger.error(s"Caught unexpected exception: ${t.getMessage}", t)
        }
      } finally {
        mostRequestsHaveBeenPosted.countDown()
      }
    }
  }

  private[this] class CompleteRequestWithResponse(slot: Slot[EnquireLinkResponse, EnquireLink])
    extends Runnable {

    override def run(): Unit = {
      val randomSleepTime = new Random().nextInt(100) + 2
      Thread.sleep(randomSleepTime)
      slot.release(slot.request.createResponse(0))
    }
  }

}
