package io.garuda.codec.pdu.tlv

import io.garuda.codec.pdu.{Decoder, Encodeable}
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 25.08.13
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
case class Tlv(tag: Tag, value: Vector[Byte]) extends Encodeable {

  def this(tag: Tag, value: Array[Byte]) = this(tag, value.toVector)

  def this(tag: Tag) = this(tag, Vector.empty[Byte])

  def unsignedLength: Int = value.length

  def length: Short = unsignedLength.toShort

  def lengthInBytes: Int = tag.lengthInBytes + 2 + unsignedLength

  def encodeInto(channelBuffer: ByteBuf): Unit = {
    tag.encodeInto(channelBuffer)
    channelBuffer.writeShort(length)
    channelBuffer.writeBytes(value.toArray)
  }
}

object Tlv extends Decoder[Tlv] {

  val MinimumLength: Int = 4

  def apply(tag: Tag, value: Array[Byte]) = new Tlv(tag, value)

  def apply(tag: Tag, value: Byte) = new Tlv(tag, Array[Byte](value))

  def apply(tag: Tag) = new Tlv(tag)

  def decodeFrom(channelBuffer: ByteBuf): Tlv = {
    require(channelBuffer.readableBytes() >= MinimumLength, "Not enough readable bytes in supplied ChannelBuffer to " +
      "decode Tlv: " + channelBuffer.readableBytes())
    val tag = Tag.decodeFrom(channelBuffer)
    val length = channelBuffer.readShort()
    val bytesValue = new Array[Byte](length)
    channelBuffer.readBytes(bytesValue)
    Tlv(tag, bytesValue)
  }
}
