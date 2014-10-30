package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 24.09.13
 * Time: 23:01
 * To change this template use File | Settings | File Templates.
 */
case class GenericNack(commandStatus: Int,
                       seqNo: Option[Int],
                       rawPdu: Option[Array[Byte]]) extends PduResponse(CommandId.generic_nack,
  commandStatus,
  seqNo,
  Vector.empty[Tlv],
  rawPdu: Option[Array[Byte]]) {

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {}

  protected[this] def bodyLengthInBytes: Int = 0
}

object GenericNack extends PduDecoder[GenericNack] {

  def apply(commandStatus: Int, seqNo: Int, rawPdu: Array[Byte]): GenericNack = {
    GenericNack(commandStatus, Some(seqNo), Some(rawPdu))
  }

  def apply(commandStatus: Int, seqNo: Int, resultMessage: String): GenericNack = {
    GenericNack(commandStatus, Some(seqNo), None)
  }

  def apply(commandStatus: Int, seqNo: Int): GenericNack = {
    GenericNack(commandStatus, Some(seqNo), None)
  }

  def apply(commandStatus: Int): GenericNack = {
    GenericNack(commandStatus, None, None)
  }

  protected class GenericNackBody extends Body[GenericNack]

  type BODY = GenericNackBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): GenericNackBody = {
    new GenericNackBody
  }

  protected[this] def makePduFrom(header: Header, body: GenericNack.GenericNackBody,
                                  tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): GenericNack = {
    require(tlvParameters.isEmpty, "GenericNack does not support optional TLVs")
    GenericNack(header.commandStatus, header.sequenceNumber, Some(rawPdu))
  }
}
