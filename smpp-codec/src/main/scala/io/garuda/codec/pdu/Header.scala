package io.garuda.codec.pdu

import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 18.08.13
 * Time: 12:31
 * To change this template use File | Settings | File Templates.
 */
case class Header(commandLength: Int, commandId: CommandId, commandStatus: Int,
                  sequenceNumber: Option[Int]) extends Encodeable {

  def encodeInto(channelBuffer: ByteBuf) {
    if (sequenceNumber.isEmpty) throw new IllegalStateException("Illegal attempt to encode a Header without assigned " +
      "sequence number")
    channelBuffer.writeInt(commandLength)
    channelBuffer.writeInt(commandId.asInt)
    channelBuffer.writeInt(commandStatus)
    channelBuffer.writeInt(sequenceNumber.get)
  }

  protected[pdu] def lengthInBytes: Int = Header.Length
}

object Header {

  val Length = 16

  def apply(commandLength: Int, commandId: CommandId, commandStatus: Int, sequenceNumber: Int): Header =
    Header(commandLength,
      commandId,
      commandStatus,
      Some(sequenceNumber))

  def getFrom(channelBuffer: ByteBuf): Option[Header] = {
    if (channelBuffer.readableBytes() < Length) return None
    val offset = channelBuffer.readerIndex()
    val commandLength = channelBuffer.getInt(offset)
    require(commandLength >= Length, "Decoded command_lenght is below minimum command_lenght (16): " + commandLength)
    val commandId = channelBuffer.getInt(offset + 4)
    val commandStatus = channelBuffer.getInt(offset + 8)
    val sequenceNumber = channelBuffer.getInt(offset + 12)
    Some(Header(commandLength, CommandId(commandId), commandStatus, sequenceNumber))
  }
}
