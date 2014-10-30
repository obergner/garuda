package io.garuda.common.logging.slf4j

import org.slf4j.MDC

import scala.collection.JavaConversions._
import scala.language.implicitConversions

/**
 * Created by obergner on 19.05.14.
 */
final class MdcPropagatingRunnableWrapper(wrapped: Runnable) extends Runnable {
  require(wrapped != null, "Wrapped Runnable must not be null")

  private[this] val callerContextMap: java.util.Map[_, _] = MDC.getCopyOfContextMap

  override def run(): Unit = {
    val oldContextMap = MDC.getCopyOfContextMap
    try {
      MDC.setContextMap(callerContextMap)
      wrapped.run()
    } finally {
      MDC.setContextMap(oldContextMap)
    }
  }
}

object MdcPropagatingRunnableWrapper {

  def apply(wrapped: Runnable): MdcPropagatingRunnableWrapper = new MdcPropagatingRunnableWrapper(wrapped)

  def wrap(runnables: java.util.Collection[Runnable]): java.util.Collection[Runnable] = {
    require(runnables != null, "Argument 'runnables' must not be null")
    val wrapped = new java.util.ArrayList[Runnable](runnables.size())
    runnables foreach { runnable => wrapped.add(MdcPropagatingRunnableWrapper(runnable))}
    wrapped
  }
}
