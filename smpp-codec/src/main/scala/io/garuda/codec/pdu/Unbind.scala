package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 24.09.13
 * Time: 22:54
 * To change this template use File | Settings | File Templates.
 */
case class Unbind(seqNo: Option[Int],
                  rawPdu: Option[Array[Byte]] = None) extends PduRequest[UnbindResponse](CommandId.unbind,
  seqNo,
  Vector.empty[Tlv],
  rawPdu) {

  def createResponse(commandStatus: Int): UnbindResponse =
    UnbindResponse(commandStatus, sequenceNumber, None)

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {}

  protected[this] def bodyLengthInBytes: Int = 0
}

object Unbind extends PduDecoder[Unbind] {

  def apply(seqNo: Int, rawPdu: Array[Byte]): Unbind = Unbind(Some(seqNo), Some(rawPdu))

  def apply(seqNo: Int): Unbind = Unbind(Some(seqNo), None)

  def apply(): Unbind = Unbind(None, None)

  protected class UnbindBody extends Body[Unbind]

  type BODY = UnbindBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): Unbind.UnbindBody = new Unbind.UnbindBody

  protected[this] def makePduFrom(header: Header, body: Unbind.UnbindBody, tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): Unbind = {
    require(tlvParameters.isEmpty, "Unbind does not support optional TLVs")
    Unbind(header.sequenceNumber, Some(rawPdu))
  }
}
