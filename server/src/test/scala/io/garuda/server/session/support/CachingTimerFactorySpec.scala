package io.garuda.server.session.support

import java.util.concurrent.{CyclicBarrier, ExecutorService, Executors}

import io.garuda.common.builder.BuilderContext
import io.garuda.server.session.CommonServerSessionConfig
import io.netty.util.Timer
import io.netty.util.internal.ConcurrentSet
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Specification, Tags}
import org.specs2.specification.AroundOutside
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

/**
 * Created by obergner on 28.03.14.
 */
class CachingTimerFactorySpec extends Specification with Tags with NoTimeConversions {

  case class builder_context(name: String) extends AroundOutside[BuilderContext] {

    private[this] lazy val builderContext = BuilderContext.withDefaultMetricRegistry(name, 10 milliseconds)

    override def outside: BuilderContext = builderContext

    override def around[R](a: => R)(implicit evidence$3: AsResult[R]): Result = {
      try {
        outside.open()
        AsResult(a)
      } finally {
        outside.close()
      }
    }
  }

  "CachingTimerFactory" should {

    "create a new Timer every numberOfSessionsPerTimer invocations (single-threaded)" in
      new builder_context("create a new Timer every numberOfSessionsPerTimer invocations (single-threaded)") {
        (builderContext: BuilderContext) => {
          val numberOfSessionsPerTimer = 10
          val expectedNumberOfCreatedTimers = 100
          val createdTimers = scala.collection.mutable.Set[Timer]()

          val objectUnderTest = new CachingTimerFactory("single-threaded", numberOfSessionsPerTimer)

          (0 until expectedNumberOfCreatedTimers * numberOfSessionsPerTimer) foreach {
            _ => createdTimers += objectUnderTest(CommonServerSessionConfig.default(builderContext))
          }

          createdTimers.size must_== expectedNumberOfCreatedTimers
        }
      } tag "unit"

    "create a new Timer every numberOfSessionsPerTimer invocations (multi-threaded/high-contention)" in
      new
          builder_context("create a new Timer every numberOfSessionsPerTimer invocations (multi-threaded/high-contention)") {
        (builderContext: BuilderContext) => {
          val numberOfConcurrentThreads = 60
          val numberOfInvocationsPerThread = 100
          val numberOfSessionsPerTimer = 3
          val expectedNumberOfCreatedTimers = (numberOfConcurrentThreads * numberOfInvocationsPerThread) / numberOfSessionsPerTimer
          val createdTimers = new ConcurrentSet[Timer]()

          val objectUnderTest = new CachingTimerFactory("single-threaded", numberOfSessionsPerTimer)

          val startStopBarrier: CyclicBarrier = new CyclicBarrier(numberOfConcurrentThreads + 1)
          val executorService: ExecutorService = Executors.newFixedThreadPool(numberOfConcurrentThreads)
          (0 until numberOfConcurrentThreads) foreach {
            _ => executorService.execute(new CachingThreadFactoryInvoker(objectUnderTest,
              startStopBarrier,
              createdTimers,
              numberOfInvocationsPerThread,
              builderContext))
          }
          startStopBarrier.await()
          startStopBarrier.await()
          executorService.shutdownNow()

          createdTimers.size must_== expectedNumberOfCreatedTimers
        }
      } tag "multithreaded"
  }

  private class CachingThreadFactoryInvoker(objectUnderTest: CachingTimerFactory,
                                            startStopBarrier: CyclicBarrier,
                                            createdTimers: ConcurrentSet[Timer],
                                            numberOfInvocations: Int,
                                            builderContext: BuilderContext) extends Runnable {
    override def run(): Unit = {
      startStopBarrier.await()

      (0 until numberOfInvocations) foreach {
        _ => createdTimers.add(objectUnderTest(CommonServerSessionConfig.default(builderContext)))
      }

      startStopBarrier.await()
    }
  }

}
