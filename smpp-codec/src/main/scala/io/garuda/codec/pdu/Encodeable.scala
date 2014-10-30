package io.garuda.codec.pdu

import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 17.08.13
 * Time: 12:53
 * To change this template use File | Settings | File Templates.
 */
trait Encodeable extends EncodingSupport {

  def encodeInto(channelBuffer: ByteBuf): Unit
}
