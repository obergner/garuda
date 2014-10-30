package io.garuda.common

/**
 * Created by obergner on 26.03.14.
 */
package object logging {

  /**
   * Key used to store the current [[io.netty.channel.Channel]]'s ID in the current thread's [[org.slf4j.MDC]].
   */
  val ChannelContextKey: String = "channel"

  /**
   * Key used to store the current [[io.garuda.common.authentication.System]]'s ID in the current thread's [[org.slf4j.MDC]].
   */
  val RemoteSystemIdContextKey: String = "remoteSystemId"
}
