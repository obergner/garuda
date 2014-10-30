package io.garuda.common.concurrent

import com.typesafe.scalalogging.slf4j.Logging

import scala.concurrent.ExecutionContext

/**
 * An [[ExecutionContext]] that executes tasks on the caller's thread.
 */
object CallerThreadExecutionContext extends ExecutionContext with Logging {

  /**
   * Execute the given [[Runnable]] on the caller's [[Thread]].
   *
   * @param runnable The [[Runnable]] to execute
   * @throws IllegalArgumentException If `runnable` is `null`
   */
  @throws[IllegalArgumentException]("If runnable is null")
  override def execute(runnable: Runnable): Unit = {
    require(runnable != null, "Argument 'runnable' must not be null")
    runnable.run()
  }

  /**
   *
   * @param t
   */
  override def reportFailure(t: Throwable): Unit = logger.error(s"Caught error running task: ${t.getMessage}", t)
}
