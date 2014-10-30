package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 23.09.13
 * Time: 21:31
 * To change this template use File | Settings | File Templates.
 */
case class EnquireLinkResponse(commandStatus: Int,
                               seqNo: Option[Int],
                               rawPdu: Option[Array[Byte]]) extends PduResponse(CommandId.enquire_link_resp,
  commandStatus,
  seqNo,
  Vector.empty[Tlv],
  rawPdu: Option[Array[Byte]]) {

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {}

  protected[this] def bodyLengthInBytes: Int = 0
}

object EnquireLinkResponse extends PduDecoder[EnquireLinkResponse] {

  def apply(commandStatus: Int, seqNo: Int, rawPdu: Array[Byte]): EnquireLinkResponse = {
    EnquireLinkResponse(commandStatus, Some(seqNo), Some(rawPdu))
  }

  def apply(commandStatus: Int, seqNo: Int): EnquireLinkResponse = {
    EnquireLinkResponse(commandStatus, Some(seqNo), None)
  }

  def apply(commandStatus: Int): EnquireLinkResponse = {
    EnquireLinkResponse(commandStatus, None, None)
  }

  protected class EnquireLinkResponseBody extends Body[EnquireLinkResponse]

  type BODY = EnquireLinkResponseBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): EnquireLinkResponseBody = {
    new EnquireLinkResponseBody
  }

  protected[this] def makePduFrom(header: Header, body: EnquireLinkResponse.EnquireLinkResponseBody,
                                  tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): EnquireLinkResponse = {
    require(tlvParameters.isEmpty, "EnquireLinkResponse does not support optional TLVs")
    EnquireLinkResponse(header.commandStatus, header.sequenceNumber, Some(rawPdu))
  }
}
