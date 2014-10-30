package io.garuda.windowing.support

import com.codahale.metrics.MetricRegistry
import io.garuda.windowing.{WindowConfig, WindowFactory}

/**
 * Created by obergner on 16.05.14.
 */
object SemaphoreGuardedWindowFactory extends WindowFactory[SemaphoreGuardedWindow] {

  override def apply(windowId: String,
                     windowConfig: WindowConfig,
                     metricRegistry: MetricRegistry): SemaphoreGuardedWindow = {
    new SemaphoreGuardedWindow(windowId, windowConfig, metricRegistry)
  }
}
