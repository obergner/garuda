package io.garuda.common.spi.session

import io.garuda.codec.pdu._
import io.netty.channel.ChannelHandlerContext

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 13.11.13
 * Time: 20:55
 * To change this template use File | Settings | File Templates.
 */
trait SessionChannelAdapter {

  /**
   * Callback invoked when the underlying [[io.netty.channel.Channel]] has been `activated`.
   *
   * @param ctx The `activated` [[io.netty.channel.Channel]]'s [[ChannelHandlerContext]]
   */
  def channelActivated(ctx: ChannelHandlerContext): Unit

  /**
   * Callback invoked when the underlying [[io.netty.channel.Channel]] has received a [[BaseBind]] request.
   *
   * @param ctx The underlying [[io.netty.channel.Channel]]'s [[ChannelHandlerContext]]
   * @param bindRequest The received [[BaseBind]]
   */
  def bindRequestReceived(ctx: ChannelHandlerContext, bindRequest: BaseBind[_ <: BaseBindResponse]): Unit

  /**
   * Callback invoked when the underlying [[io.netty.channel.Channel]] has received an [[Unbind]] request.
   *
   * @param ctx The underlying [[io.netty.channel.Channel]]'s [[ChannelHandlerContext]]
   * @param unbindRequest
   */
  def unbindRequestReceived(ctx: ChannelHandlerContext, unbindRequest: Unbind): Unit

  /**
   * Callback invoked when the underlying [[io.netty.channel.Channel]] has received an [[EnquireLink]] request.
   *
   * @param ctx The underlying [[io.netty.channel.Channel]]'s [[ChannelHandlerContext]]
   * @param enquireLinkRequest
   */
  def enquireLinkRequestReceived(ctx: ChannelHandlerContext, enquireLinkRequest: EnquireLink): Unit

  /**
   * Callback invoked when the underlying [[io.netty.channel.Channel]] has received a "regular" [[PduRequest]], i.e.
   * a [[PduRequest]] that is not a session management operation.
   *
   * @param ctx The underlying [[io.netty.channel.Channel]]'s [[ChannelHandlerContext]]
   * @param pduRequest The received [[PduRequest]]
   * @tparam P The type of [[PduResponse]] expected in response to `pduRequest`
   * @tparam R The type of [[PduRequest]]
   */
  def pduRequestReceived[P <: PduResponse, R <: PduRequest[P]](ctx: ChannelHandlerContext, pduRequest: R): Unit

  /**
   * Callback invoked when the underlying [[io.netty.channel.Channel]] has been closed.
   *
   * @param ctx The underlying [[io.netty.channel.Channel]]'s [[ChannelHandlerContext]]
   */
  def channelClosed(ctx: ChannelHandlerContext): Unit
}
