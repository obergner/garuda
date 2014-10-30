package io.garuda.codec.pdu

import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 17.08.13
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
trait Decoder[T] extends DecodingSupport {

  def decodeFrom(channelBuffer: ByteBuf): T
}
