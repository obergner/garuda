package io.garuda.common.resources

import java.util
import java.util.Collections
import java.util.concurrent.{Callable, ExecutorService, Executors, Future}

import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.slf4j.Logging
import io.netty.util.{HashedWheelTimer, Timer}
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Specification, Tags}
import org.specs2.specification.AroundOutside
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

/**
 * Created by obergner on 06.09.14.
 */
class ResourcesRegistrySpec extends Specification with Tags with NoTimeConversions with Logging {

  logger.info(s"Initializing logging subsystem for ${this}")

  case class resources_registry(name: String, shutdownTimeout: Duration) extends AroundOutside[ResourcesRegistry] {

    private[this] lazy val objectUnderTest = new ResourcesRegistry(name, shutdownTimeout, new MetricRegistry)

    override def outside: ResourcesRegistry = objectUnderTest

    override def around[R](a: => R)(implicit evidence$3: AsResult[R]): Result = {
      try {
        AsResult(a)
      } finally {
        outside.stop()
      }
    }
  }

  private object NoopExecutorService extends ExecutorService {
    override def shutdown(): Unit = {}

    override def isTerminated: Boolean = true

    override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true

    override def shutdownNow(): util.List[Runnable] = Collections.emptyList()

    override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]): util.List[Future[T]] = Collections.emptyList()

    override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): util.List[Future[T]] = Collections.emptyList()

    override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]): T = null.asInstanceOf[T]

    override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T = null.asInstanceOf[T]

    override def isShutdown: Boolean = true

    override def submit[T](task: Callable[T]): Future[T] = null

    override def submit[T](task: Runnable, result: T): Future[T] = null

    override def submit(task: Runnable): Future[_] = null

    override def execute(command: Runnable): Unit = {}
  }

  "ResourcesRegistry#registerTimer(ExecutorService)" should {

    "reject null ExecutorService" in {
      val objectUnderTest = new ResourcesRegistry("reject null ExecutorService", 10 milliseconds, new MetricRegistry)
      objectUnderTest.registerExecutorService(null.asInstanceOf[ExecutorService]) must throwAn[IllegalArgumentException]
    } tag "unit"

  }

  "ResourcesRegistry#registerTimer(Timer)" should {

    "reject null Timer" in {
      val objectUnderTest = new ResourcesRegistry("reject null Timer", 10 milliseconds, new MetricRegistry)
      objectUnderTest.registerTimer(null.asInstanceOf[Timer]) must throwAn[IllegalArgumentException]
    } tag "unit"
  }

  "ResourcesRegistry#stop()" should {

    "shutdown registered ExecutorService" in new
        resources_registry("shutdown registered ExecutorService", 10 milliseconds) {
      (objectUnderTest: ResourcesRegistry) => {
        val executorService = Executors.newCachedThreadPool()
        objectUnderTest.start()
        objectUnderTest.registerExecutorService(executorService)
        objectUnderTest.stop()
        executorService.isShutdown must beTrue
      }
    } tag "integration"


    "shutdown ExecutorService registered via wrapper function" in new
        resources_registry("shutdown ExecutorService registered via wrapper function", 10 milliseconds) {
      (objectUnderTest: ResourcesRegistry) => {
        val executorService = Executors.newCachedThreadPool()
        val executorServiceFac: Int => ExecutorService = Function.const(executorService)
        objectUnderTest.start()
        val wrapperFac = objectUnderTest.registerExecutorService(executorServiceFac)
        wrapperFac(1)
        objectUnderTest.stop()
        executorService.isShutdown must beTrue
      }
    } tag "integration"

    "stop registered Timer" in new resources_registry("stop registered Timer", 10 milliseconds) {
      (objectUnderTest: ResourcesRegistry) => {
        val timer = new HashedWheelTimer()
        timer.start()
        objectUnderTest.start()
        objectUnderTest.registerTimer(timer)
        objectUnderTest.stop()
        timer.start() must throwAn[IllegalStateException]
      }
    } tag "integration"

    "stop Timer registered via wrapper function" in new
        resources_registry("stop Timer registered via wrapper function", 10 milliseconds) {
      (objectUnderTest: ResourcesRegistry) => {
        val timer = new HashedWheelTimer()
        val timerFac: (Int, String) => Timer = (i: Int, s: String) => timer
        timer.start()
        objectUnderTest.start()
        val wrapperFac = objectUnderTest.registerTimer(timerFac)
        wrapperFac(2, "Hello")
        objectUnderTest.stop()
        timer.start() must throwAn[IllegalStateException]
      }
    } tag "integration"
  }
}
