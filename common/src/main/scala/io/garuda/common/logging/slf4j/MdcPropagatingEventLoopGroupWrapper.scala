package io.garuda.common.logging.slf4j

import java.util
import java.util.concurrent.{Callable, Future, TimeUnit}

import io.netty.channel.{Channel, ChannelFuture, ChannelPromise, EventLoop, EventLoopGroup}
import io.netty.util.concurrent
import io.netty.util.concurrent.{EventExecutor, ScheduledFuture}

/**
 * Created by obergner on 19.05.14.
 */
class MdcPropagatingEventLoopGroupWrapper(wrapped: EventLoopGroup) extends EventLoopGroup {

  override def next(): EventLoop = wrapped.next()

  override def register(channel: Channel): ChannelFuture = wrapped.register(channel)

  override def register(channel: Channel, promise: ChannelPromise): ChannelFuture = wrapped.register(channel, promise)

  override def shutdown(): Unit = wrapped.shutdown()

  override def scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): ScheduledFuture[_] =
    wrapped.scheduleAtFixedRate(MdcPropagatingRunnableWrapper(command), initialDelay, period, unit)

  override def schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture[_] =
    wrapped.schedule(MdcPropagatingRunnableWrapper(command), delay, unit)

  override def schedule[V](callable: Callable[V], delay: Long, unit: TimeUnit): ScheduledFuture[V] =
    wrapped.schedule[V](MdcPropagatingCallableWrapper(callable), delay, unit)

  override def shutdownNow(): util.List[Runnable] = wrapped.shutdownNow()

  override def isShuttingDown: Boolean = wrapped.isShuttingDown

  override def terminationFuture(): concurrent.Future[_] = wrapped.terminationFuture()

  override def iterator(): util.Iterator[EventExecutor] = wrapped.iterator()

  override def shutdownGracefully(): concurrent.Future[_] = wrapped.shutdownGracefully()

  override def shutdownGracefully(quietPeriod: Long, timeout: Long, unit: TimeUnit): concurrent.Future[_] =
    wrapped.shutdownGracefully(quietPeriod, timeout, unit)

  override def scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): ScheduledFuture[_] =
    wrapped.scheduleWithFixedDelay(MdcPropagatingRunnableWrapper(command), initialDelay, delay, unit)

  override def submit(task: Runnable): concurrent.Future[_] = wrapped.submit(MdcPropagatingRunnableWrapper(task))

  override def submit[T](task: Runnable, result: T): concurrent.Future[T] =
    wrapped.submit[T](MdcPropagatingRunnableWrapper(task), result)

  override def submit[T](task: Callable[T]): concurrent.Future[T] =
    wrapped.submit[T](MdcPropagatingCallableWrapper[T, Callable[T]](task))

  override def isTerminated: Boolean = wrapped.isTerminated

  override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = wrapped.awaitTermination(timeout, unit)

  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]): util.List[Future[T]] =
    wrapped.invokeAll[T](MdcPropagatingCallableWrapper.wrap(tasks))

  override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): util.List[Future[T]] =
    wrapped.invokeAll[T](MdcPropagatingCallableWrapper.wrap(tasks), timeout, unit)

  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]): T =
    wrapped.invokeAny[T](MdcPropagatingCallableWrapper.wrap(tasks))

  override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T =
    wrapped.invokeAny[T](MdcPropagatingCallableWrapper.wrap(tasks), timeout, unit)

  override def isShutdown: Boolean = wrapped.isShutdown

  override def execute(command: Runnable): Unit = wrapped.execute(MdcPropagatingRunnableWrapper(command))
}
