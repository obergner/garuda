package io.garuda.codec

import io.garuda.codec.pdu.Pdu
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 12.10.13
 * Time: 15:56
 * To change this template use File | Settings | File Templates.
 */
class SmppPduEncoder {

  def encodeInto(pdu: Pdu, channelBuffer: ByteBuf): Unit = {
    pdu.encodeInto(channelBuffer)
  }
}
