package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.Tlv
import io.netty.buffer.ByteBuf

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 06.10.13
 * Time: 13:48
 * To change this template use File | Settings | File Templates.
 */
case class SubmitSmResponse(commandStatus: Int,
                            seqNo: Option[Int],
                            messageId: String,
                            tlvParameters: Vector[Tlv] = Vector.empty[Tlv],
                            rawPdu: Option[Array[Byte]] = None) extends PduResponse(CommandId.submit_sm_resp,
  commandStatus,
  seqNo,
  tlvParameters,
  rawPdu) {

  protected[this] def encodeBodyInto(channelBuffer: ByteBuf): Unit = {
    encodeNullTerminatedStringInto(messageId, channelBuffer)
  }

  protected[this] def bodyLengthInBytes: Int = messageId.length + 1
}

object SubmitSmResponse extends PduDecoder[SubmitSmResponse] {

  protected case class SubmitSmBody(messageId: String) extends Body[SubmitSmResponse]

  type BODY = SubmitSmBody

  protected[this] def decodeBodyFrom(channelBuffer: ByteBuf): SubmitSmResponse.BODY = {
    var messageId = readMessageIdFrom(channelBuffer)
    SubmitSmBody(messageId)
  }

  protected[this] def makePduFrom(header: Header, body: SubmitSmResponse.BODY, tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): SubmitSmResponse = {
    SubmitSmResponse(header.commandStatus, header.sequenceNumber, body.messageId, tlvParameters, Some(rawPdu))
  }
}
