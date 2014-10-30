package io.garuda.codec.pdu

import io.garuda.codec.pdu.tlv.{Tag, Tlv}

/**
 * Created with IntelliJ IDEA.
 * User: obergner
 * Date: 22.09.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
case class BindTransceiverResponse(commandStatus: Int,
                                   seqNo: Option[Int],
                                   override val systemId: String,
                                   tlvParameters: Vector[Tlv] = Vector.empty[Tlv],
                                   rawPdu: Option[Array[Byte]] = None) extends BaseBindResponse(CommandId
  .bind_transceiver_resp,
  commandStatus,
  seqNo,
  systemId,
  tlvParameters,
  rawPdu)

object BindTransceiverResponse extends BaseBindResponseDecoder[BindTransceiverResponse] {

  def apply(commandStatus: Int,
            systemId: String,
            tlvParameters: Vector[Tlv]): BindTransceiverResponse = {
    new BindTransceiverResponse(commandStatus, None, systemId, tlvParameters)
  }

  def apply(commandStatus: Int,
            sequenceNumber: Int,
            systemId: String,
            tlvParameters: Vector[Tlv]): BindTransceiverResponse = {
    new BindTransceiverResponse(commandStatus, Some(sequenceNumber), systemId, tlvParameters)
  }

  def apply(commandStatus: Int,
            systemId: String,
            interfaceVersion: InterfaceVersion): BindTransceiverResponse = {
    new BindTransceiverResponse(commandStatus, None, systemId,
      Vector[Tlv](Tlv(Tag.ScInterfaceVersion,
        interfaceVersion
          .asByte)))
  }


  def apply(commandStatus: Int,
            sequenceNumber: Int,
            systemId: String,
            interfaceVersion: InterfaceVersion): BindTransceiverResponse = {
    new BindTransceiverResponse(commandStatus, Some(sequenceNumber), systemId,
      Vector[Tlv](Tlv(Tag.ScInterfaceVersion,
        interfaceVersion.asByte)))
  }

  protected[this] def makePduFrom(header: Header,
                                  body: BindTransceiverResponse.BindResponseBody,
                                  tlvParameters: Vector[Tlv],
                                  rawPdu: Array[Byte]): BindTransceiverResponse = BindTransceiverResponse(header
    .commandStatus,
    header
      .sequenceNumber,
    body.systemId,
    tlvParameters,
    Some(rawPdu))
}
