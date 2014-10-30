package io.garuda.common.logging.slf4j

import io.garuda.common.logging._
import io.netty.channel.{ChannelFuture, ChannelFutureListener}
import org.slf4j.MDC

/**
 * A generic [[ChannelFutureListener]] implementation that will first register the current [[io.netty.channel.Channel]]
 * in SLF4J's [[MDC]] for logging purposes, before subsequently executing a callback passed in in its constructor. After
 * callback completion, it will unregister the current [[io.netty.channel.Channel]] from the [[MDC]].
 *
 * Created by obergner on 30.03.14.
 *
 * @param callback The callback to execute
 */
final class StoreChannelInMdcChannelFutureListener(callback: ChannelFuture => Unit) extends ChannelFutureListener {

  override def operationComplete(future: ChannelFuture): Unit = {
    try {
      MDC.put(ChannelContextKey, future.channel().toString)
      callback(future)
    } finally {
      MDC.remove(ChannelContextKey)
    }
  }
}

object StoreChannelInMdcChannelFutureListener {

  def apply(callback: ChannelFuture => Unit): StoreChannelInMdcChannelFutureListener = new
      StoreChannelInMdcChannelFutureListener(callback)
}
