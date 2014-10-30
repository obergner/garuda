package io.garuda.codec.util

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled._

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 13.08.13
 * Time: 21:06
 * To change this template use File | Settings | File Templates.
 */
object ChannelBufferHelpers {

  val NullByte: Byte = 0x00.toByte

  def createBufferFromByteArray(byteArray: Array[Byte]): ByteBuf = copiedBuffer(byteArray)

  def createBufferFromHexString(hexString: String): ByteBuf =
    createBufferFromByteArray(HexHelpers.hexStringToByteArray(hexString))

  def createNullTerminatedBufferFromString(string: String): ByteBuf =
    createBufferFromByteArray(string.getBytes("ISO-8859-1") :+ NullByte)
}
