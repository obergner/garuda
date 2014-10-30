package io.garuda.common

import java.net.InetSocketAddress

import io.netty.channel.Channel

/**
 * Abstract base class for exceptions to be thrown whenever a problem with a [[io.netty.channel.Channel]] or its
 * underlying TCP/IP connection occurs.
 *
 * User: obergner
 * Date: 29.10.13
 * Time: 22:25
 * To change this template use File | Settings | File Templates.
 */
abstract class ChannelException(message: String, cause: Throwable, channel: Channel)
  extends RuntimeException(message, cause) {

  def this(channel: Channel) = this("", null, channel)

  def this(message: String, channel: Channel) = this(message, null, channel)

  def this(cause: Throwable, channel: Channel) = this("", cause, channel)

  def remoteAddress: InetSocketAddress = channel.remoteAddress().asInstanceOf[InetSocketAddress]

  def localAddress: InetSocketAddress = channel.localAddress().asInstanceOf[InetSocketAddress]
}
