package io.garuda.common.logging

import java.util.concurrent.atomic.AtomicBoolean

import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 18.10.13
 * Time: 23:47
 * To change this template use File | Settings | File Templates.
 */
package object slf4j {

  val nettyInternalLoggerHasBeenSet = new AtomicBoolean(false)

  def setupNettyInternalLogger(): Unit = {
    if (nettyInternalLoggerHasBeenSet.compareAndSet(false, true)) {
      val slf4jLoggerFactory: InternalLoggerFactory = new Slf4JLoggerFactory()
      InternalLoggerFactory.setDefaultFactory(slf4jLoggerFactory)
    }
  }
}
