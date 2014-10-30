package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 23.09.13
 * Time: 22:28
 * To change this template use File | Settings | File Templates.
 */
case class EnquireLink(seqNo: Option[Int],
                       rawPdu: Option[Array[Byte]] = None) extends PduRequest[EnquireLinkResponse](CommandId
  .enquire_link,
  seqNo,
  Vector.empty[Tlv],
  rawPdu) {

  def createResponse(commandStatus: Int): EnquireLinkResponse =
    EnquireLinkResponse(commandStatus, sequenceNumber, None)

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {}

  protected[this] def bodyLengthInBytes: Int = 0
}

object EnquireLink extends PduDecoder[EnquireLink] {

  def apply(seqNo: Int, rawPdu: Array[Byte]): EnquireLink = EnquireLink(Some(seqNo), Some(rawPdu))

  def apply(seqNo: Int): EnquireLink = EnquireLink(Some(seqNo), None)

  def apply(): EnquireLink = EnquireLink(None, None)

  protected class EnquireLinkBody extends Body[EnquireLink]

  type BODY = EnquireLinkBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): EnquireLink.EnquireLinkBody = new EnquireLink
  .EnquireLinkBody

  protected[this] def makePduFrom(header: Header, body: EnquireLink.EnquireLinkBody, tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): EnquireLink = {
    require(tlvParameters.isEmpty, "EnquireLink does not support optional TLVs")
    EnquireLink(header.sequenceNumber, Some(rawPdu))
  }
}
