package io.garuda.common.logging.slf4j

import io.garuda.common.logging._
import io.netty.channel.Channel
import org.slf4j.MDC

import scala.concurrent.ExecutionContext

/**
 * Created by obergner on 17.05.14.
 */
final class ChannelRegisteringExecutionContextWrapper(channel: Channel, wrapped: ExecutionContext)
  extends ExecutionContext {

  override def execute(runnable: Runnable): Unit = wrapped.execute(new ChannelRegisteringRunnableWrapper(runnable))

  override def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)

  private[this] final class ChannelRegisteringRunnableWrapper(wrappedRunnable: Runnable) extends Runnable {

    override def run(): Unit = {
      try {
        MDC.put(ChannelContextKey, channel.toString)
        wrappedRunnable.run()
      } finally {
        MDC.remove(ChannelContextKey)
      }
    }
  }

}
