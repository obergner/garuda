package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 24.09.13
 * Time: 22:46
 * To change this template use File | Settings | File Templates.
 */
case class UnbindResponse(commandStatus: Int,
                          seqNo: Option[Int],
                          rawPdu: Option[Array[Byte]]) extends PduResponse(CommandId.unbind_resp,
  commandStatus,
  seqNo,
  Vector.empty[Tlv],
  rawPdu: Option[Array[Byte]]) {

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {}

  protected[this] def bodyLengthInBytes: Int = 0
}

object UnbindResponse extends PduDecoder[UnbindResponse] {

  def apply(commandStatus: Int, seqNo: Int, rawPdu: Array[Byte]): UnbindResponse = {
    UnbindResponse(commandStatus, Some(seqNo), Some(rawPdu))
  }

  def apply(commandStatus: Int, seqNo: Int): UnbindResponse = {
    UnbindResponse(commandStatus, Some(seqNo), None)
  }

  def apply(commandStatus: Int): UnbindResponse = {
    UnbindResponse(commandStatus, None, None)
  }

  def apply(): UnbindResponse = {
    UnbindResponse(0, None, None)
  }

  protected class UnbindResponseBody extends Body[UnbindResponse]

  type BODY = UnbindResponseBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): UnbindResponseBody = {
    new UnbindResponseBody
  }

  protected[this] def makePduFrom(header: Header, body: UnbindResponse.UnbindResponseBody,
                                  tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): UnbindResponse = {
    require(tlvParameters.isEmpty, "UnbindResponse does not support optional TLVs")
    UnbindResponse(header.commandStatus, header.sequenceNumber, Some(rawPdu))
  }
}
