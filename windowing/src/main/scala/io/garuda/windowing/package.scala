package io.garuda

import com.codahale.metrics.MetricRegistry

/**
 * Created by obergner on 18.05.14.
 */
package object windowing {

  type WindowFactory[T <: Window] = (String, WindowConfig, MetricRegistry) => T
}
