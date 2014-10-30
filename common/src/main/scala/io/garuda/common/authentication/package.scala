package io.garuda.common

import io.netty.util.AttributeKey

/**
 * Created by obergner on 11.10.14.
 */
package object authentication {

  /**
   * [[AttributeKey]] to attache the remote [[RemoteSystem]] to the current [[io.netty.channel.Channel]].
   */
  val RemoteSystemAttributeKey: AttributeKey[RemoteSystem] = AttributeKey.valueOf("remoteSystem")
}
