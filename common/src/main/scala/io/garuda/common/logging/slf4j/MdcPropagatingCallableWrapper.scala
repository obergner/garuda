package io.garuda.common.logging.slf4j

import java.util.concurrent.Callable

import org.slf4j.MDC

import scala.collection.JavaConversions._
import scala.language.implicitConversions

/**
 * Created by obergner on 19.05.14.
 */
final class MdcPropagatingCallableWrapper[V, T <: Callable[V]](wrapped: T) extends Callable[V] {
  require(wrapped != null, "Wrapped Callable must not be null")

  private[this] val callerContextMap: java.util.Map[_, _] = MDC.getCopyOfContextMap

  override def call(): V = {
    val oldContextMap = MDC.getCopyOfContextMap
    try {
      MDC.setContextMap(callerContextMap)
      wrapped.call()
    } finally {
      MDC.setContextMap(oldContextMap)
    }
  }
}

object MdcPropagatingCallableWrapper {

  def apply[V, T <: Callable[V]](wrapped: T): Callable[V] = new MdcPropagatingCallableWrapper[V, T](wrapped)

  def wrap[V](callables: java.util.Collection[_ <: Callable[V]]): java.util.Collection[Callable[V]] = {
    require(callables != null, "Argument 'callables' must not be null")
    val wrapped = new java.util.ArrayList[Callable[V]](callables.size())
    callables foreach { callable => wrapped.add(MdcPropagatingCallableWrapper(callable))}
    wrapped
  }
}
