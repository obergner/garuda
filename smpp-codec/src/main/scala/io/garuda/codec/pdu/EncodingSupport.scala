package io.garuda.codec.pdu

import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 12.10.13
 * Time: 11:11
 * To change this template use File | Settings | File Templates.
 */
abstract class EncodingSupport {

  protected[this] def encodeNullTerminatedStringInto(string: String, channelBuffer: ByteBuf): Unit = {
    val stringBytes: Array[Byte] = string.getBytes("ISO-8859-1")
    channelBuffer.writeBytes(stringBytes)
    channelBuffer.writeByte(0x00.toByte)
  }

}
