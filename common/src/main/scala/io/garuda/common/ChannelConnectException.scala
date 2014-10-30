package io.garuda.common

import io.netty.channel.Channel

/**
 * Thrown whenever an SMPP client or server fails to connect to its remote peer.
 *
 * User: obergner
 * Date: 29.10.13
 * Time: 21:50
 * To change this template use File | Settings | File Templates.
 */
class ChannelConnectException(message: String, cause: Throwable, channel: Channel)
  extends ChannelException(message, cause, channel) {

  def this(channel: Channel) = this("", null, channel)

  def this(message: String, channel: Channel) = this(message, null, channel)

  def this(cause: Throwable, channel: Channel) = this("", cause, channel)

}
